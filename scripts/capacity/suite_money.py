"""Suite A — max ledger $ volume (single transfer + settled multi-chunk)."""

from __future__ import annotations

import time
from pathlib import Path
from typing import Any

from lib_classify import classify_failure
from lib_http import ApiClient
from lib_metrics import LatencyTracker, Stopwatch, write_json
from lib_seed import seed_funded_wallets

# Probe amounts (ledger units). Binary-search between last-good and first-fail.
# Sparse geometric ladder — each step pays a Maven seeder cold-start.
PROBE_AMOUNTS = [
    10,
    1_000,
    10_000,  # MoneyFlowTest Kafka note
    100_000,
    1_000_000,
    1_000_000_000,
    1_000_000_000_000,  # 1e12
    10_000_000_000_000,  # 10 trillion (BaseTest ideal)
    9_223_372_036_854_775_807,  # Long.MAX_VALUE
]


def _wait_ready(api: ApiClient, attempts: int = 30) -> bool:
    for _ in range(attempts):
        if api.ready().ok:
            return True
        time.sleep(1)
    return False


def _create_dest(api: ApiClient) -> dict[str, Any]:
    r = api.create_wallet()
    if not r.ok or not isinstance(r.body, dict):
        raise RuntimeError(f"create dest failed: status={r.status} err={r.error}")
    return r.body


def _settle_once(
    api: ApiClient,
    src_uid: str,
    dest_addr: str,
    amount: int,
) -> dict[str, Any]:
    """Transact → mine → read balances. Returns outcome dict."""
    lat = LatencyTracker()
    tx = api.transact(src_uid, dest_addr, amount)
    lat.add(tx.elapsed_s)
    if not tx.ok:
        return {
            "ok": False,
            "phase": "transact",
            "status": tx.status,
            "error": tx.error,
            "latency": lat.summary(),
            "balance_mismatch": False,
        }

    mine = api.mine()
    lat.add(mine.elapsed_s)
    if not mine.ok:
        return {
            "ok": False,
            "phase": "mine",
            "status": mine.status,
            "error": mine.error,
            "latency": lat.summary(),
            "balance_mismatch": False,
            "kafka_hint": "kafka" in (mine.error or "").lower()
            or "kafka" in (mine.text or "").lower(),
        }

    # Small settle window for async wallet updates
    time.sleep(0.5)
    info = api.wallet_info(src_uid)
    lat.add(info.elapsed_s)
    if not info.ok or not isinstance(info.body, dict):
        return {
            "ok": False,
            "phase": "wallet_info",
            "status": info.status,
            "error": info.error,
            "latency": lat.summary(),
            "balance_mismatch": False,
        }

    # Dest balance via create response address — getByUid needs dest uid.
    # We only assert src decreased if we know prior balance; caller checks.
    return {
        "ok": True,
        "phase": "settled",
        "status": 200,
        "error": None,
        "latency": lat.summary(),
        "src_balance": info.body.get("balance"),
        "balance_mismatch": False,
    }


def _try_amount(api: ApiClient, amount: int) -> dict[str, Any]:
    """Seed treasury with amount+buffer, transfer `amount` to fresh dest, mine."""
    # Seed slightly above amount so balance check is meaningful; clamp for Long.MAX
    if amount >= 9_223_372_036_854_775_807:
        seed_balance = amount
        send = amount  # will fail on insufficient if we also need fees/reward — no fee
    else:
        seed_balance = amount
        send = amount

    try:
        wallets = seed_funded_wallets(1, seed_balance)
    except Exception as exc:  # noqa: BLE001
        return {
            "ok": False,
            "phase": "seed",
            "amount": amount,
            "status": 0,
            "error": str(exc),
            "balance_mismatch": False,
            "amount_near_long_max": amount >= 9_000_000_000_000_000_000,
        }

    src = wallets[0]
    dest = _create_dest(api)
    dest_addr = dest["address"]
    dest_uid = dest.get("uid")

    outcome = _settle_once(api, src["uid"], dest_addr, send)
    outcome["amount"] = amount
    outcome["src_uid"] = src["uid"]
    outcome["dest_uid"] = dest_uid
    outcome["amount_near_long_max"] = amount >= 9_000_000_000_000_000_000

    if outcome.get("ok") and dest_uid:
        dinfo = api.wallet_info(dest_uid)
        if dinfo.ok and isinstance(dinfo.body, dict):
            got = dinfo.body.get("balance")
            # Parse as int via str — never float() (IEEE-754 loses integers > 2^53-1).
            try:
                got_n = int(str(got).split(".")[0]) if got is not None else None
            except (TypeError, ValueError):
                got_n = None
            if got_n is not None and got_n != amount:
                outcome["ok"] = False
                outcome["balance_mismatch"] = True
                outcome["phase"] = "balance_check"
                outcome["error"] = f"dest balance {got_n} != expected {amount}"
                outcome["dest_balance"] = got_n
            else:
                outcome["dest_balance"] = got_n
        else:
            outcome["ok"] = False
            outcome["phase"] = "dest_info"
            outcome["error"] = dinfo.error
            outcome["status"] = dinfo.status

    kafka_blob = f"{outcome.get('error') or ''} {outcome.get('phase')}"
    outcome["kafka_hint"] = outcome.get("kafka_hint") or (
        "kafka" in kafka_blob.lower() or amount >= 10_000 and not outcome.get("ok")
    )
    return outcome


def _binary_search_ceiling(
    api: ApiClient,
    low_good: int,
    high_fail: int,
    max_steps: int = 12,
) -> tuple[int, int, list[dict[str, Any]]]:
    """Return (last_good, first_fail, trial_log)."""
    log: list[dict[str, Any]] = []
    last_good = low_good
    first_fail = high_fail
    lo, hi = low_good, high_fail
    for _ in range(max_steps):
        if hi - lo <= 1:
            break
        mid = (lo + hi) // 2
        if mid <= lo or mid >= hi:
            break
        if not _wait_ready(api):
            log.append({"amount": mid, "ok": False, "phase": "ready", "error": "not ready"})
            break
        trial = _try_amount(api, mid)
        log.append(trial)
        if trial.get("ok"):
            last_good = mid
            lo = mid
        else:
            first_fail = mid
            hi = mid
    return last_good, first_fail, log


def _settled_volume_ramp(
    api: ApiClient,
    chunk: int,
    max_chunks: int,
) -> dict[str, Any]:
    """Settle many chunks of `chunk` from one treasury until failure."""
    total_needed = chunk * max_chunks
    wallets = seed_funded_wallets(1, total_needed)
    src = wallets[0]
    dest = _create_dest(api)
    settled = 0
    accepted = 0
    lat = LatencyTracker()
    failures: list[dict[str, Any]] = []
    for i in range(max_chunks):
        t0 = time.perf_counter()
        tx = api.transact(src["uid"], dest["address"], chunk)
        lat.add(time.perf_counter() - t0)
        if not tx.ok:
            failures.append({"i": i, "phase": "transact", "error": tx.error, "status": tx.status})
            break
        accepted += 1

    # Drain bounded settlement batches after the acceptance ramp so this suite
    # tests aggregate volume without paying one mining round-trip per chunk.
    for i in range((accepted // 500) + 3):
        t0 = time.perf_counter()
        mine = api.mine()
        lat.add(time.perf_counter() - t0)
        if mine.status == 204:
            break
        if not mine.ok:
            failures.append({"i": i, "phase": "mine", "error": mine.error, "status": mine.status})
            break
        count = (
            int(mine.body.get("transactionCount", 0))
            if isinstance(mine.body, dict) else 0
        )
        settled += count * chunk

    destination_info = api.wallet_info(dest["uid"])
    destination_balance = (
        destination_info.body.get("balance")
        if destination_info.ok and isinstance(destination_info.body, dict)
        else None
    )
    expected = accepted * chunk
    if destination_balance is None or int(str(destination_balance).split(".")[0]) != expected:
        failures.append({
            "phase": "balance_check",
            "expected": expected,
            "actual": destination_balance,
        })
    return {
        "chunk": chunk,
        "max_chunks": max_chunks,
        "chunks_accepted": accepted,
        "settled_volume": settled,
        "chunks_ok": settled // chunk if chunk else 0,
        "destination_balance": destination_balance,
        "latency": lat.summary(),
        "failures": failures,
        "ok": len(failures) == 0 and settled == expected,
    }


def run_suite_money(
    api: ApiClient,
    out_dir: Path,
    *,
    volume_chunk: int = 1_000,
    volume_chunks: int = 1_000,
) -> dict[str, Any]:
    sw = Stopwatch()
    trials: list[dict[str, Any]] = []
    last_good: int | None = None
    first_fail: int | None = None
    first_fail_trial: dict[str, Any] | None = None

    for amount in PROBE_AMOUNTS:
        if not _wait_ready(api):
            trials.append({"amount": amount, "ok": False, "phase": "ready", "error": "API not ready"})
            first_fail = amount
            first_fail_trial = trials[-1]
            break
        trial = _try_amount(api, amount)
        trials.append(trial)
        if trial.get("ok"):
            last_good = amount
        else:
            first_fail = amount
            first_fail_trial = trial
            break

    binary_log: list[dict[str, Any]] = []
    if last_good is not None and first_fail is not None and first_fail > last_good + 1:
        lg, ff, binary_log = _binary_search_ceiling(api, last_good, first_fail)
        last_good, first_fail = lg, ff
        trials.extend(binary_log)

    # Escalated aggregate volume (1,000 x 1,000 = 1,000,000 by default).
    volume_result: dict[str, Any] | None = None
    if last_good and last_good >= 10:
        chunk = min(volume_chunk, last_good)
        volume_result = _settled_volume_ramp(
            api, chunk=chunk, max_chunks=volume_chunks)

    limitations: list[dict[str, Any]] = []
    if first_fail_trial is not None:
        limitations.append(
            classify_failure(
                suite="A_money",
                metric="single_transfer_amount",
                last_good=last_good,
                first_fail=first_fail,
                signal=f"phase={first_fail_trial.get('phase')} error={first_fail_trial.get('error')}",
                http_status=first_fail_trial.get("status"),
                error_text=str(first_fail_trial.get("error") or ""),
                balance_mismatch=bool(first_fail_trial.get("balance_mismatch")),
                amount_near_long_max=bool(first_fail_trial.get("amount_near_long_max")),
                kafka_hint=bool(first_fail_trial.get("kafka_hint")),
                evidence_files=[str(out_dir / "suite_money.json")],
            )
        )

    result = {
        "suite": "A_money",
        "elapsed_s": sw.elapsed(),
        "last_good_single_amount": last_good,
        "first_fail_single_amount": first_fail,
        "trials": trials,
        "binary_trials": binary_log,
        "settled_volume": volume_result,
        "limitations": limitations,
    }
    write_json(out_dir / "suite_money.json", result)
    return result
