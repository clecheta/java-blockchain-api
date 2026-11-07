"""Suite C — max host throughput (multi-wallet transact + periodic mine)."""

from __future__ import annotations

import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path
from typing import Any

from lib_classify import classify_failure
from lib_http import ApiClient
from lib_metrics import LatencyTracker, Stopwatch, sample_host, write_json
from lib_seed import seed_funded_wallets

DEFAULT_DURATION_S = 180.0
DEFAULT_WALLET_PAIRS = 200
DEFAULT_SEED_BALANCE = 10_000
DEFAULT_TX_AMOUNT = 1
DEFAULT_MINE_EVERY = 500
ERROR_RATE_STOP = 0.15


def _wait_ready(api: ApiClient, attempts: int = 20) -> bool:
    for _ in range(attempts):
        if api.ready().ok:
            return True
        time.sleep(1)
    return False


def run_suite_throughput(
    api: ApiClient,
    out_dir: Path,
    mongo_uri: str,
    *,
    duration_s: float = DEFAULT_DURATION_S,
    wallet_pairs: int = DEFAULT_WALLET_PAIRS,
    seed_balance: int = DEFAULT_SEED_BALANCE,
    tx_amount: int = DEFAULT_TX_AMOUNT,
    workers: int = 128,
    mine_every: int = DEFAULT_MINE_EVERY,
    evidence_file: str = "suite_throughput.json",
) -> dict[str, Any]:
    if mine_every <= 0:
        raise ValueError("mine_every must be positive")
    sw = Stopwatch()
    if not _wait_ready(api):
        result = {
            "suite": "C_throughput",
            "ok": False,
            "error": "API not ready",
            "limitations": [
                classify_failure(
                    suite="C_throughput",
                    metric="tx_per_s",
                    last_good=None,
                    first_fail=None,
                    signal="API not ready before throughput suite",
                    ready_ok=False,
                    evidence_files=[str(out_dir / evidence_file)],
                )
            ],
        }
        write_json(out_dir / evidence_file, result)
        return result

    # Seed N funded senders; create N HTTP destinations (balance 0).
    senders = seed_funded_wallets(wallet_pairs, seed_balance, mongo_uri=mongo_uri)
    dests: list[dict[str, Any]] = []
    for i in range(wallet_pairs):
        r = api.create_wallet(f"capacity-tp-dest-{i:04d}-{time.time_ns()}")
        if not r.ok or not isinstance(r.body, dict):
            raise RuntimeError(f"dest create failed: {r.error}")
        dests.append(r.body)

    pairs = list(zip(senders, dests, strict=True))
    # Track remaining spendable per sender (approx; mine settles)
    remaining = {s["uid"]: seed_balance for s in senders}

    tx_lat = LatencyTracker()
    mine_lat = LatencyTracker()
    tx_ok = 0
    tx_fail = 0
    mine_ok = 0
    mine_fail = 0
    mine_skipped = 0
    settled_units = 0
    host_samples: list[dict[str, Any]] = []
    errors: list[dict[str, Any]] = []
    pending_since_mine = 0
    stop_reason = "duration"
    mine_executor = ThreadPoolExecutor(max_workers=1)
    mine_future = None

    def _mine(batch_units: int) -> dict[str, Any]:
        with ApiClient(base_url, timeout_s=120.0) as mine_client:
            t0 = time.perf_counter()
            result = mine_client.mine()
            return {
                "result": result,
                "elapsed_s": time.perf_counter() - t0,
                "batch_units": batch_units,
            }

    def _record_mine(item: dict[str, Any]) -> int:
        nonlocal mine_ok, mine_fail, mine_skipped, settled_units
        result = item["result"]
        mine_lat.add(item["elapsed_s"])
        if result.status == 200:
            mine_ok += 1
            count = (
                int(result.body.get("transactionCount", 0))
                if isinstance(result.body, dict) else 0
            )
            settled_units += count * tx_amount
            return count
        elif result.status == 204:
            mine_skipped += 1
        else:
            mine_fail += 1
            if len(errors) < 50:
                errors.append({
                    "phase": "mine",
                    "status": result.status,
                    "error": str(result.error or result.text or "")[:2000],
                })
        return 0

    base_url = api.base_url
    deadline = time.perf_counter() + duration_s
    round_idx = 0
    next_health_check = 0.0

    load_started = time.perf_counter()
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

    # Reuse the HTTP connection pool and worker pool for the entire run.
    # Creating a client and executor per request round measures the harness,
    # not the API.
    with ThreadPoolExecutor(max_workers=min(workers, len(pairs))) as pool:
        while time.perf_counter() < deadline:
            if mine_future is not None and mine_future.done():
                _record_mine(mine_future.result())
                mine_future = None

            now = time.perf_counter()
            if now >= next_health_check:
                if not api.ready().ok:
                    stop_reason = "ready_failed"
                    break
                host_samples.append(sample_host())
                next_health_check = now + 1.0

            # Multiple pending transfers per sender are intentional: acceptance
            # throughput and periodic batch settlement are measured separately.
            batch = [
                (s, d)
                for s, d in pairs
                if remaining.get(s["uid"], 0) >= tx_amount
            ]
            if not batch:
                stop_reason = "funds_exhausted"
                break

            futs = [pool.submit(_one, p) for p in batch]
            for fut in as_completed(futs):
                item = fut.result()
                tx_lat.add(item["elapsed_s"])
                if item["ok"]:
                    tx_ok += 1
                    pending_since_mine += 1
                    remaining[item["uid"]] = remaining.get(item["uid"], 0) - tx_amount
                else:
                    tx_fail += 1
                    if len(errors) < 50:
                        errors.append({"phase": "transact", **item})

            # Settle a larger batch periodically. Mining every HTTP transfer
            # would measure settlement latency, not acceptance capacity.
            if pending_since_mine >= mine_every and mine_future is None:
                batch_units = pending_since_mine
                pending_since_mine = 0
                mine_future = mine_executor.submit(_mine, batch_units)

            total = tx_ok + tx_fail
            if total >= 40 and (tx_fail / total) > ERROR_RATE_STOP:
                stop_reason = "error_rate"
                break

            round_idx += 1

    accept_elapsed = max(time.perf_counter() - load_started, 1e-6)

    # Drain the in-flight settlement, then settle leftovers.
    if mine_future is not None:
        _record_mine(mine_future.result())
    max_drain_attempts = (max(tx_ok - settled_units, 0) // mine_every) + 2
    for _ in range(max_drain_attempts):
        if settled_units >= tx_ok:
            break
        if _record_mine(_mine(tx_ok - settled_units)) == 0:
            break
    pending_since_mine = max(tx_ok - settled_units, 0)
    mine_executor.shutdown(wait=True)

    elapsed = max(sw.elapsed(), 1e-6)
    settlement_elapsed = max(time.perf_counter() - load_started, 1e-6)
    tx_per_s = tx_ok / accept_elapsed
    mine_per_s = mine_ok / settlement_elapsed
    dollars_per_s = settled_units / settlement_elapsed
    total_tx = tx_ok + tx_fail
    err_rate = (tx_fail / total_tx) if total_tx else 0.0

    # Too few senders can still serialize cryptographic signing per account.
    sender_contention = workers > wallet_pairs

    err0 = errors[0] if errors else {}
    err_blob = " ".join(str(e.get("error", "")) for e in errors)
    limitations: list[dict[str, Any]] = [
        classify_failure(
            suite="C_throughput",
            metric="tx_per_s",
            last_good=round(tx_per_s, 4) if stop_reason == "duration" else None,
            first_fail=round(tx_per_s, 4) if stop_reason != "duration" else None,
            signal=(
                f"stop={stop_reason}; tx_ok={tx_ok} tx_fail={tx_fail} "
                f"err_rate={err_rate:.3f}; pairs={wallet_pairs} workers={workers}"
            ),
            error_text=str(err0.get("error") or stop_reason),
            http_status=err0.get("status"),
            ready_ok=stop_reason != "ready_failed",
            host_saturated=bool(
                host_samples and (host_samples[-1].get("load1") or 0) > 8
            ),
            sender_contention=sender_contention,
            mine_required=True,
            kafka_hint="kafka" in err_blob.lower(),
            evidence_files=[str(out_dir / evidence_file)],
        )
    ]
    # Always record architecture settlement constraint observed in this suite
    limitations.append(
        classify_failure(
            suite="C_throughput",
            metric="settlement_gate",
            last_good=mine_ok,
            first_fail=None,
            signal=(
                "Transfers are accepted continuously and settled in periodic batches; "
                f"observed mine_ok={mine_ok} mine_fail={mine_fail} mine_every={mine_every}"
            ),
            mine_required=True,
            sender_contention=True,
            evidence_files=[str(out_dir / evidence_file)],
        )
    )

    result = {
        "suite": "C_throughput",
        "elapsed_s": elapsed,
        "accept_window_s": accept_elapsed,
        "settlement_window_s": settlement_elapsed,
        "duration_requested_s": duration_s,
        "stop_reason": stop_reason,
        "wallet_pairs": wallet_pairs,
        "workers": workers,
        "mine_every": mine_every,
        "tx_amount": tx_amount,
        "seed_balance": seed_balance,
        "tx_ok": tx_ok,
        "tx_fail": tx_fail,
        "mine_ok": mine_ok,
        "mine_fail": mine_fail,
        "mine_skipped": mine_skipped,
        "settled_units": settled_units,
        "tx_per_s": tx_per_s,
        "mine_per_s": mine_per_s,
        "dollars_per_s": dollars_per_s,
        "error_rate": err_rate,
        "tx_latency": tx_lat.summary(),
        "mine_latency": mine_lat.summary(),
        "host_samples": host_samples,
        "errors": errors,
        "rounds": round_idx,
        "limitations": limitations,
    }
    write_json(out_dir / evidence_file, result)
    return result
