"""V5 account-volume stage: keep N accounts present and settle a moved total.

Completion ignores wall-clock time and host saturation. The stage stops only when
settled volume reaches the target with correctness checks, or when correctness
fails. Synthetic bulk rows provide account population; ECDSA wallets carry the
money path (fund → transact → mine).
"""

from __future__ import annotations

import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from decimal import Decimal, InvalidOperation
from pathlib import Path
from typing import Any

from lib_http import ApiClient
from lib_metrics import LatencyTracker, Stopwatch, sample_host, write_json
from lib_planes import _docker_psql, wallet_count
from lib_seed import seed_funded_wallets
from suite_populate_accounts import run_suite_populate_accounts

DEFAULT_ACCOUNTS = 5_000_000
DEFAULT_MOVED = 150_000_000_000
DEFAULT_TX_AMOUNT = 1_000_000
DEFAULT_PAIRS = 200
DEFAULT_WORKERS = 64
DEFAULT_MINE_EVERY = 500
RETRY_SLEEP_S = 0.5
INSUFFICIENT_MARKERS = (
    "amount exceeds available balance",
    "insufficient",
)
ACQUISITION_MARKERS = (
    "acquisition timeout",
    "unable to acquire jdbc connection",
    "sorry, acquisition timeout",
)


def _wait_ready(api: ApiClient, attempts: int = 60) -> bool:
    for _ in range(attempts):
        if api.ready().ok:
            return True
        time.sleep(1)
    return False


def _is_retryable(status: int, error: str | None) -> bool:
    if status in {0, 429, 502, 503, 504}:
        return True
    text = (error or "").lower()
    if any(marker in text for marker in ACQUISITION_MARKERS):
        return True
    return "timeout" in text or "connection" in text or "temporarily" in text


def _is_insufficient(status: int, error: str | None) -> bool:
    if status not in {400, 409}:
        return False
    text = (error or "").lower()
    return any(marker in text for marker in INSUFFICIENT_MARKERS)


def _parse_balance(raw: Any) -> Decimal:
    if raw is None:
        return Decimal("0")
    try:
        return Decimal(str(raw))
    except (InvalidOperation, ValueError):
        return Decimal("0")


def _correctness_snapshot(api: ApiClient, dest_uids: list[str]) -> dict[str, Any]:
    wallets = wallet_count()
    negatives = int(
        _docker_psql(
            "SELECT count(*) FROM ledger_wallet "
            "WHERE balance < 0 OR reserved_balance < 0;"
        )
        or "0"
    )
    dest_balance_sum = Decimal("0")
    for uid in dest_uids:
        info = api.wallet_info(uid)
        if info.ok and isinstance(info.body, dict):
            dest_balance_sum += _parse_balance(info.body.get("balance"))
    return {
        "wallet_count": wallets,
        "negative_balance_rows": negatives,
        "destination_balance_sum": str(dest_balance_sum),
    }


def run_suite_volume_stage(
    api: ApiClient,
    out_dir: Path,
    *,
    accounts: int = DEFAULT_ACCOUNTS,
    moved: int = DEFAULT_MOVED,
    tx_amount: int = DEFAULT_TX_AMOUNT,
    pairs: int = DEFAULT_PAIRS,
    workers: int = DEFAULT_WORKERS,
    mine_every: int = DEFAULT_MINE_EVERY,
    evidence_file: str = "suite_volume_stage.json",
) -> dict[str, Any]:
    if accounts <= 0:
        raise ValueError("accounts must be positive")
    if moved <= 0:
        raise ValueError("moved must be positive")
    if tx_amount <= 0:
        raise ValueError("tx_amount must be positive")
    if pairs <= 0:
        raise ValueError("pairs must be positive")
    if mine_every <= 0:
        raise ValueError("mine_every must be positive")
    if moved % tx_amount != 0:
        raise ValueError("moved must be divisible by tx_amount")

    sw = Stopwatch()
    if not _wait_ready(api):
        result = {
            "suite": "V_volume_stage",
            "ok": False,
            "stop_reason": "correctness_failed",
            "error": "API not ready",
            "accounts_target": accounts,
            "moved_target": moved,
        }
        write_json(out_dir / evidence_file, result)
        return result

    populate_result: dict[str, Any] | None = None
    before_count = wallet_count()
    if before_count < accounts:
        print(f"==> Populate synthetic accounts → {accounts} (have {before_count})")
        populate_result = run_suite_populate_accounts(out_dir, target_count=accounts)
        if not populate_result.get("ok"):
            result = {
                "suite": "V_volume_stage",
                "ok": False,
                "stop_reason": "correctness_failed",
                "error": "populate did not reach account target",
                "accounts_target": accounts,
                "moved_target": moved,
                "populate": populate_result,
            }
            write_json(out_dir / evidence_file, result)
            return result

    transfers_needed = moved // tx_amount
    per_sender_transfers = (transfers_needed + pairs - 1) // pairs
    seed_balance = per_sender_transfers * tx_amount

    print(
        f"==> Seed {pairs} funded senders "
        f"(seed_balance={seed_balance}, tx_amount={tx_amount})"
    )
    senders = seed_funded_wallets(pairs, seed_balance)
    dests: list[dict[str, Any]] = []
    for i in range(pairs):
        r = api.create_wallet(f"capacity-v5-dest-{i:04d}-{time.time_ns()}")
        if not r.ok or not isinstance(r.body, dict):
            raise RuntimeError(f"dest create failed: {r.error}")
        dests.append(r.body)
    pair_list = list(zip(senders, dests, strict=True))
    remaining = {s["uid"]: seed_balance for s in senders}
    dest_uids = [str(d["uid"]) for d in dests]

    accepted = 0
    settled = 0
    tx_fail_retries = 0
    fund_topups = 0
    mine_ok = 0
    mine_fail = 0
    pending_since_mine = 0
    host_samples: list[dict[str, Any]] = []
    errors: list[dict[str, Any]] = []
    tx_lat = LatencyTracker()
    mine_lat = LatencyTracker()
    stop_reason: str | None = None
    abort = False
    base_url = api.base_url
    next_health = 0.0
    mine_executor = ThreadPoolExecutor(max_workers=1)
    mine_future = None

    def _mine(batch_hint: int) -> dict[str, Any]:
        with ApiClient(base_url, timeout_s=300.0) as mine_client:
            t0 = time.perf_counter()
            result = mine_client.mine()
            return {
                "result": result,
                "elapsed_s": time.perf_counter() - t0,
                "batch_hint": batch_hint,
            }

    def _record_mine(item: dict[str, Any]) -> int:
        nonlocal mine_ok, mine_fail, settled
        result = item["result"]
        mine_lat.add(item["elapsed_s"])
        if result.status == 200:
            mine_ok += 1
            count = (
                int(result.body.get("transactionCount", 0))
                if isinstance(result.body, dict)
                else 0
            )
            settled += count * tx_amount
            return count
        if result.status == 204:
            return 0
        mine_fail += 1
        if len(errors) < 50:
            errors.append({
                "phase": "mine",
                "status": result.status,
                "error": str(result.error or result.text or "")[:2000],
            })
        return 0

    def _fund_sender(uid: str, amount: int) -> bool:
        nonlocal fund_topups
        with ApiClient(base_url, timeout_s=120.0) as fund_client:
            funded = fund_client.fund(uid, amount)
        if funded.ok:
            fund_topups += 1
            remaining[uid] = remaining.get(uid, 0) + amount
            return True
        if len(errors) < 50:
            errors.append({
                "phase": "fund",
                "status": funded.status,
                "error": str(funded.error or funded.text or "")[:2000],
                "uid": uid,
            })
        return False

    def _one(pair: tuple[dict[str, Any], dict[str, Any]]) -> dict[str, Any]:
        src, dest = pair
        r = api.transact(src["uid"], dest["address"], tx_amount)
        return {
            "ok": r.ok,
            "status": r.status,
            "elapsed_s": r.elapsed_s,
            "error": r.error,
            "uid": src["uid"],
        }

    load_started = time.perf_counter()
    worker_n = min(workers, pairs)
    try:
        with ThreadPoolExecutor(max_workers=worker_n) as pool:
            while settled < moved and not abort:
                if mine_future is not None and mine_future.done():
                    _record_mine(mine_future.result())
                    mine_future = None

                now = time.perf_counter()
                if now >= next_health:
                    if not api.ready().ok:
                        abort = True
                        stop_reason = "correctness_failed"
                        errors.append({
                            "phase": "ready",
                            "error": "API not ready during volume stage",
                        })
                        break
                    host_samples.append(sample_host())
                    next_health = now + 5.0

                still_needed_accepts = max(0, (moved // tx_amount) - accepted)
                if still_needed_accepts == 0:
                    if pending_since_mine > 0 and mine_future is None:
                        batch_units = pending_since_mine
                        pending_since_mine = 0
                        mine_future = mine_executor.submit(_mine, batch_units)
                    elif mine_future is None:
                        break
                    time.sleep(0.05)
                    continue

                batch = [
                    (s, d)
                    for s, d in pair_list
                    if remaining.get(s["uid"], 0) >= tx_amount
                ]
                if not batch:
                    topped = False
                    for sender in senders:
                        uid = sender["uid"]
                        if remaining.get(uid, 0) < tx_amount:
                            if _fund_sender(uid, seed_balance):
                                topped = True
                            else:
                                abort = True
                                stop_reason = "correctness_failed"
                                break
                    if abort:
                        break
                    if not topped:
                        time.sleep(RETRY_SLEEP_S)
                    continue

                batch = batch[:still_needed_accepts]
                futs = [pool.submit(_one, p) for p in batch]
                for fut in as_completed(futs):
                    item = fut.result()
                    tx_lat.add(item["elapsed_s"])
                    if item["ok"]:
                        accepted += 1
                        pending_since_mine += 1
                        remaining[item["uid"]] = remaining.get(item["uid"], 0) - tx_amount
                        continue

                    if _is_retryable(item["status"], item.get("error")):
                        tx_fail_retries += 1
                        time.sleep(RETRY_SLEEP_S)
                        continue

                    if _is_insufficient(item["status"], item.get("error")):
                        remaining[item["uid"]] = 0
                        if not _fund_sender(item["uid"], seed_balance):
                            abort = True
                            stop_reason = "correctness_failed"
                            break
                        continue

                    if len(errors) < 50:
                        errors.append({"phase": "transact", **item})
                    abort = True
                    stop_reason = "correctness_failed"
                    break

                if abort:
                    break

                if pending_since_mine >= mine_every and mine_future is None:
                    batch_units = pending_since_mine
                    pending_since_mine = 0
                    mine_future = mine_executor.submit(_mine, batch_units)

        if mine_future is not None:
            _record_mine(mine_future.result())
            mine_future = None

        for _ in range((max(accepted - (settled // tx_amount), 0) // mine_every) + 5):
            if settled >= moved:
                break
            if settled // tx_amount >= accepted:
                break
            counted = _record_mine(_mine(accepted - (settled // tx_amount)))
            if counted == 0:
                break
    finally:
        mine_executor.shutdown(wait=True)

    snapshot = _correctness_snapshot(api, dest_uids)
    dest_sum = Decimal(snapshot["destination_balance_sum"])
    accounts_ok = snapshot["wallet_count"] >= accounts
    negatives_ok = snapshot["negative_balance_rows"] == 0
    volume_ok = settled >= moved and dest_sum >= Decimal(moved)
    ok = volume_ok and accounts_ok and negatives_ok and not abort
    stop_reason = "target_reached" if ok else "correctness_failed"

    result = {
        "suite": "V_volume_stage",
        "ok": ok,
        "stop_reason": stop_reason,
        "elapsed_s": sw.elapsed(),
        "accept_window_s": max(time.perf_counter() - load_started, 1e-6),
        "accounts_target": accounts,
        "accounts_present": snapshot["wallet_count"],
        "moved_target": moved,
        "settled_volume": settled,
        "accepted_transfers": accepted,
        "accepted_volume": accepted * tx_amount,
        "tx_amount": tx_amount,
        "pairs": pairs,
        "workers": workers,
        "mine_every": mine_every,
        "seed_balance_per_sender": seed_balance,
        "tx_fail_retries": tx_fail_retries,
        "fund_topups": fund_topups,
        "mine_ok": mine_ok,
        "mine_fail": mine_fail,
        "tx_latency": tx_lat.summary(),
        "mine_latency": mine_lat.summary(),
        "host_samples": host_samples[-20:],
        "correctness": snapshot,
        "populate": populate_result,
        "errors": errors,
        "note": (
            "Synthetic bulk rows establish account population pressure. "
            "Settled volume is measured on ECDSA fund → transact → mine path. "
            "Duration and host saturation are not stop conditions."
        ),
    }
    write_json(out_dir / evidence_file, result)
    return result
