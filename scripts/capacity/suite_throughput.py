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

DEFAULT_DURATION_S = 90.0
DEFAULT_WALLET_PAIRS = 20
DEFAULT_SEED_BALANCE = 10_000
DEFAULT_TX_AMOUNT = 1
MINE_EVERY = 10
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
    workers: int = 8,
) -> dict[str, Any]:
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
                    evidence_files=[str(out_dir / "suite_throughput.json")],
                )
            ],
        }
        write_json(out_dir / "suite_throughput.json", result)
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
    settled_units = 0
    host_samples: list[dict[str, Any]] = []
    errors: list[dict[str, Any]] = []
    pending_since_mine = 0
    stop_reason = "duration"

    base_url = api.base_url
    deadline = time.perf_counter() + duration_s
    round_idx = 0

    while time.perf_counter() < deadline:
        if not api.ready().ok:
            stop_reason = "ready_failed"
            break
        host_samples.append(sample_host())

        # One transfer per sender that still has balance (respects one-pending-per-sender
        # by mining frequently and using distinct senders in parallel).
        batch = [
            (s, d)
            for s, d in pairs
            if remaining.get(s["uid"], 0) >= tx_amount
        ]
        if not batch:
            stop_reason = "funds_exhausted"
            break

        def _one(pair: tuple[dict[str, Any], dict[str, Any]]) -> dict[str, Any]:
            src, dest = pair
            with ApiClient(base_url, timeout_s=30.0) as c:
                r = c.transact(src["uid"], dest["address"], tx_amount)
                return {
                    "ok": r.ok,
                    "status": r.status,
                    "elapsed_s": r.elapsed_s,
                    "error": r.error,
                    "uid": src["uid"],
                }

        with ThreadPoolExecutor(max_workers=min(workers, len(batch))) as pool:
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

        # Mine after the batch completes. Brief pause lets Kafka consumer churn
        # settle; ConcurrentModificationException on mine is a real concurrency finding.
        if pending_since_mine >= MINE_EVERY or pending_since_mine >= len(batch):
            time.sleep(0.3)
            t0 = time.perf_counter()
            mine = api.mine()
            mine_lat.add(time.perf_counter() - t0)
            if mine.ok:
                mine_ok += 1
                settled_units += pending_since_mine * tx_amount
                pending_since_mine = 0
            else:
                mine_fail += 1
                err_text = str(mine.error or mine.text or "")
                if len(errors) < 50:
                    errors.append(
                        {
                            "phase": "mine",
                            "status": mine.status,
                            "error": err_text[:2000],
                        }
                    )
                if "ConcurrentModificationException" in err_text:
                    stop_reason = "concurrent_modification_on_mine"
                    break

        total = tx_ok + tx_fail
        if total >= 40 and (tx_fail / total) > ERROR_RATE_STOP:
            stop_reason = "error_rate"
            break

        round_idx += 1
        # Brief pause to avoid spinning the CPU only on client side
        time.sleep(0.05)

    # Final mine for leftovers
    if pending_since_mine > 0:
        mine = api.mine()
        if mine.ok:
            mine_ok += 1
            settled_units += pending_since_mine * tx_amount
            pending_since_mine = 0
        else:
            mine_fail += 1

    elapsed = max(sw.elapsed(), 1e-6)
    tx_per_s = tx_ok / elapsed
    mine_per_s = mine_ok / elapsed
    dollars_per_s = settled_units / elapsed
    total_tx = tx_ok + tx_fail
    err_rate = (tx_fail / total_tx) if total_tx else 0.0

    # Architecture signal: one pending/sender — effective parallelism ≈ wallet_pairs
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
            evidence_files=[str(out_dir / "suite_throughput.json")],
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
                "Throughput is gated by explicit mine cycles and one-pending-tx-per-sender; "
                f"observed mine_ok={mine_ok} mine_fail={mine_fail} mine_every={MINE_EVERY}"
            ),
            mine_required=True,
            sender_contention=True,
            evidence_files=[str(out_dir / "suite_throughput.json")],
        )
    )

    result = {
        "suite": "C_throughput",
        "elapsed_s": elapsed,
        "duration_requested_s": duration_s,
        "stop_reason": stop_reason,
        "wallet_pairs": wallet_pairs,
        "workers": workers,
        "tx_amount": tx_amount,
        "seed_balance": seed_balance,
        "tx_ok": tx_ok,
        "tx_fail": tx_fail,
        "mine_ok": mine_ok,
        "mine_fail": mine_fail,
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
    write_json(out_dir / "suite_throughput.json", result)
    return result
