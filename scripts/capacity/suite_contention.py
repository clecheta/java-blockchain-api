"""Suite D — same-wallet contention and reservation correctness."""

from __future__ import annotations

import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from decimal import Decimal
from pathlib import Path
from typing import Any

from lib_classify import classify_failure
from lib_http import ApiClient
from lib_metrics import LatencyTracker, Stopwatch, write_json
from lib_seed import seed_funded_wallets


def _drain(api: ApiClient, max_attempts: int) -> tuple[int, list[dict[str, Any]]]:
    settled = 0
    errors: list[dict[str, Any]] = []
    for _ in range(max_attempts):
        result = api.mine()
        if result.status == 204:
            break
        if not result.ok:
            errors.append({"status": result.status, "error": result.error})
            break
        if isinstance(result.body, dict):
            settled += int(result.body.get("transactionCount", 0))
    else:
        errors.append({"status": None, "error": "drain_exhausted_before_idle"})
    return settled, errors


def run_suite_contention(
    api: ApiClient,
    out_dir: Path,
    *,
    attempts: int = 2_000,
    workers: int = 128,
    amount: int = 1,
) -> dict[str, Any]:
    sw = Stopwatch()
    # Clear leftover durable pending from prior suites so settlement counts
    # and balances for this sender are not polluted.
    pre_settled, pre_errors = _drain(api, max_attempts=2_000)
    if pre_errors:
        raise RuntimeError(f"pre-drain failed before contention suite: {pre_errors}")

    seed_balance = attempts * amount
    source = seed_funded_wallets(
        1, seed_balance)[0]
    destination_result = api.create_wallet(
        f"capacity-contention-dest-{time.time_ns()}")
    if not destination_result.ok or not isinstance(destination_result.body, dict):
        raise RuntimeError(
            f"contention destination create failed: {destination_result.error}")
    destination = destination_result.body

    latency = LatencyTracker()
    accepted = 0
    rejected = 0
    statuses: dict[str, int] = {}

    def _send(_: int) -> dict[str, Any]:
        result = api.transact(
            source["uid"], destination["address"], amount)
        return {
            "ok": result.ok,
            "status": result.status,
            "elapsed_s": result.elapsed_s,
            "error": result.error,
        }

    with ThreadPoolExecutor(max_workers=workers) as pool:
        futures = [pool.submit(_send, index) for index in range(attempts)]
        for future in as_completed(futures):
            item = future.result()
            latency.add(item["elapsed_s"])
            statuses[str(item["status"])] = statuses.get(
                str(item["status"]), 0) + 1
            if item["ok"]:
                accepted += 1
            else:
                rejected += 1

    settled, settle_errors = _drain(
        api, max_attempts=max((accepted // 500) + 8, 32))
    source_info = api.wallet_info(source["uid"])
    balance = (
        Decimal(str(source_info.body.get("balance")))
        if source_info.ok and isinstance(source_info.body, dict)
        else None
    )
    expected = Decimal(seed_balance - accepted * amount)
    correctness_ok = (
        not settle_errors
        and settled == accepted
        and balance is not None
        and balance == expected
        and balance >= 0
    )

    limitation = classify_failure(
        suite="D_contention",
        metric="same_wallet_reservation",
        last_good=accepted if correctness_ok else None,
        first_fail=None if correctness_ok else accepted,
        signal=(
            f"attempts={attempts} accepted={accepted} rejected={rejected} "
            f"settled={settled} balance={balance} expected={expected}"
        ),
        error_text=str(settle_errors),
        sender_contention=True,
        evidence_files=[str(out_dir / "suite_contention.json")],
    )
    limitation["primary"] = "architecture"
    limitation["layer"] = "architecture"

    result = {
        "suite": "D_contention",
        "elapsed_s": sw.elapsed(),
        "attempts": attempts,
        "workers": workers,
        "amount": amount,
        "seed_balance": seed_balance,
        "pre_settled": pre_settled,
        "accepted": accepted,
        "rejected": rejected,
        "settled": settled,
        "statuses": statuses,
        "latency": latency.summary(),
        "source_balance": str(balance) if balance is not None else None,
        "expected_balance": str(expected),
        "negative_balance": bool(balance is not None and balance < 0),
        "settlement_errors": settle_errors,
        "ok": correctness_ok,
        "limitations": [limitation],
    }
    write_json(out_dir / "suite_contention.json", result)
    return result
