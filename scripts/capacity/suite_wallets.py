"""Suite B — max wallets/accounts via HTTP create ramp."""

from __future__ import annotations

import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path
from typing import Any

from lib_classify import classify_failure
from lib_http import ApiClient
from lib_metrics import LatencyTracker, Stopwatch, sample_host, write_json

# Geometric batch sizes; stop on hard fail, ready fail, or time/budget.
BATCH_SCHEDULE = [10, 25, 50, 100, 200, 500, 1000, 2000, 5000]
P95_BUDGET_MS = 5_000.0
DEFAULT_TIME_BUDGET_S = 600.0  # 10 minutes soft stop
DEFAULT_HARD_CAP = 20_000


def _wait_ready(api: ApiClient, attempts: int = 20) -> bool:
    for _ in range(attempts):
        if api.ready().ok:
            return True
        time.sleep(1)
    return False


def _create_one(base_url: str, idx: int, timeout_s: float) -> dict[str, Any]:
    with ApiClient(base_url, timeout_s=timeout_s) as client:
        user_id = f"capacity-batch-wallet-{idx:08d}-{time.time_ns()}"
        # ensure >= 20 chars
        r = client.create_wallet(user_id)
        return {
            "ok": r.ok,
            "status": r.status,
            "elapsed_s": r.elapsed_s,
            "error": r.error,
            "uid": (r.body or {}).get("uid") if isinstance(r.body, dict) else None,
        }


def run_suite_wallets(
    api: ApiClient,
    out_dir: Path,
    *,
    time_budget_s: float = DEFAULT_TIME_BUDGET_S,
    hard_cap: int = DEFAULT_HARD_CAP,
    workers: int = 8,
) -> dict[str, Any]:
    sw = Stopwatch()
    lat = LatencyTracker()
    created = 0
    batches: list[dict[str, Any]] = []
    last_good = 0
    first_fail: int | None = None
    stop_reason: str | None = None
    fail_signal = ""
    fail_status: int | None = None
    host_samples: list[dict[str, Any]] = []
    base_url = api.base_url

    for batch_size in BATCH_SCHEDULE:
        if created >= hard_cap:
            stop_reason = "hard_cap"
            break
        if sw.elapsed() >= time_budget_s:
            stop_reason = "time_budget"
            break
        if not _wait_ready(api):
            stop_reason = "ready_failed"
            first_fail = created + 1
            fail_signal = "health ready failed before batch"
            break

        remaining = hard_cap - created
        n = min(batch_size, remaining)
        host_samples.append(sample_host())
        t_batch = time.perf_counter()
        ok_count = 0
        err: dict[str, Any] | None = None

        with ThreadPoolExecutor(max_workers=workers) as pool:
            futs = [
                pool.submit(_create_one, base_url, created + i, 60.0)
                for i in range(n)
            ]
            for fut in as_completed(futs):
                item = fut.result()
                lat.add(item["elapsed_s"])
                if item["ok"]:
                    ok_count += 1
                elif err is None:
                    err = item

        batch_elapsed = time.perf_counter() - t_batch
        created += ok_count
        summary = lat.summary()
        batch_rec = {
            "requested": n,
            "ok": ok_count,
            "created_total": created,
            "batch_elapsed_s": batch_elapsed,
            "latency": summary,
            "error": err,
        }
        batches.append(batch_rec)

        p95 = summary.get("p95_ms") or 0
        if err is not None:
            first_fail = created + 1
            stop_reason = "create_failed"
            fail_signal = f"status={err.get('status')} error={err.get('error')}"
            fail_status = err.get("status")
            break
        if p95 > P95_BUDGET_MS:
            first_fail = created
            stop_reason = "p95_budget"
            fail_signal = f"p95_ms={p95} exceeded budget {P95_BUDGET_MS}"
            break
        if not api.ready().ok:
            first_fail = created
            stop_reason = "ready_failed_after_batch"
            fail_signal = "health ready failed after batch"
            break

        last_good = created

    soft_stop = stop_reason in {"time_budget", "hard_cap"} and first_fail is None
    limitations: list[dict[str, Any]] = []
    if first_fail is not None:
        host_sat = False
        if host_samples:
            last = host_samples[-1]
            load1 = last.get("load1") or 0
            host_sat = load1 > (8.0)  # rough for 12-thread box
        limitations.append(
            classify_failure(
                suite="B_wallets",
                metric="wallet_count",
                last_good=last_good,
                first_fail=first_fail,
                signal=f"{stop_reason}: {fail_signal}",
                http_status=fail_status,
                error_text=fail_signal,
                host_saturated=host_sat,
                ready_ok=stop_reason not in {"ready_failed", "ready_failed_after_batch"},
                evidence_files=[str(out_dir / "suite_wallets.json")],
            )
        )
    elif soft_stop:
        # No hard cliff — record architecture note that create@0 is fine at this scale
        limitations.append(
            classify_failure(
                suite="B_wallets",
                metric="wallet_count",
                last_good=last_good,
                first_fail=None,
                signal=f"soft_stop={stop_reason}; no hard failure within budget "
                f"(time_budget_s={time_budget_s}, hard_cap={hard_cap})",
                ready_ok=True,
                evidence_files=[str(out_dir / "suite_wallets.json")],
            )
        )
        # Override primary for soft stop: infrastructure host envelope, not a code bug
        limitations[-1]["primary"] = "infrastructure"
        limitations[-1]["layer"] = "infrastructure"
        limitations[-1]["secondary"] = ["architecture"]

    result = {
        "suite": "B_wallets",
        "elapsed_s": sw.elapsed(),
        "last_good_wallet_count": last_good,
        "first_fail_wallet_count": first_fail,
        "stop_reason": stop_reason,
        "workers": workers,
        "p95_budget_ms": P95_BUDGET_MS,
        "time_budget_s": time_budget_s,
        "hard_cap": hard_cap,
        "latency": lat.summary(),
        "batches": batches,
        "host_samples": host_samples,
        "limitations": limitations,
    }
    write_json(out_dir / "suite_wallets.json", result)
    return result
