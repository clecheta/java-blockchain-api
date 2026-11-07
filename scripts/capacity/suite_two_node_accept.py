"""Suite E — concurrent accepts on two nodes with root-only settlement."""

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


def run_suite_two_node_accept(
    root: ApiClient,
    peer_url: str,
    out_dir: Path,
    mongo_uri: str,
    *,
    attempts: int = 2_000,
    workers: int = 128,
    amount: int = 1,
) -> dict[str, Any]:
    sw = Stopwatch()
    pre_settled, pre_errors = _drain(root, max_attempts=2_000)
    if pre_errors:
        raise RuntimeError(f"pre-drain failed before two-node suite: {pre_errors}")

    seed_balance = attempts * amount
    source = seed_funded_wallets(
        1, seed_balance, mongo_uri=mongo_uri)[0]
    destination_result = root.create_wallet(
        f"capacity-two-node-dest-{time.time_ns()}")
    if not destination_result.ok or not isinstance(destination_result.body, dict):
        raise RuntimeError(
            f"two-node destination create failed: {destination_result.error}")
    destination = destination_result.body

    latency = LatencyTracker()
    accepted = 0
    rejected = 0
    per_node: dict[str, dict[str, int]] = {
        "root": {"accepted": 0, "rejected": 0},
        "peer": {"accepted": 0, "rejected": 0},
    }

    with ApiClient(peer_url, timeout_s=60.0) as peer:
        if not peer.ready().ok:
            raise RuntimeError(f"peer is not ready at {peer_url}")

        def _send(index: int) -> dict[str, Any]:
            name = "root" if index % 2 == 0 else "peer"
            client = root if name == "root" else peer
            result = client.transact(
                source["uid"], destination["address"], amount)
            return {
                "node": name,
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
                key = "accepted" if item["ok"] else "rejected"
                per_node[item["node"]][key] += 1
                if item["ok"]:
                    accepted += 1
                else:
                    rejected += 1

        peer_settlement_status = peer.mine().status
        settled, settlement_errors = _drain(
            root, max_attempts=max((accepted // 500) + 8, 32))

        source_info = root.wallet_info(source["uid"])
        balance = (
            Decimal(str(source_info.body.get("balance")))
            if source_info.ok and isinstance(source_info.body, dict)
            else None
        )
        expected = Decimal(seed_balance - accepted * amount)

        root_chain = root.chain()
        peer_tip_matches = False
        for _ in range(30):
            peer_chain = peer.chain()
            if (
                root_chain.ok
                and peer_chain.ok
                and isinstance(root_chain.body, list)
                and isinstance(peer_chain.body, list)
                and root_chain.body
                and peer_chain.body
                and root_chain.body[-1].get("hash")
                == peer_chain.body[-1].get("hash")
            ):
                peer_tip_matches = True
                break
            time.sleep(1)

    correctness_ok = (
        peer_settlement_status == 403
        and not settlement_errors
        and settled == accepted
        and balance is not None
        and balance == expected
        and balance >= 0
        and peer_tip_matches
    )
    limitation = classify_failure(
        suite="E_two_node_accept",
        metric="cross_node_reservation",
        last_good=accepted if correctness_ok else None,
        first_fail=None if correctness_ok else accepted,
        signal=(
            f"accepted={accepted} rejected={rejected} settled={settled} "
            f"peer_mine={peer_settlement_status} tip_match={peer_tip_matches}"
        ),
        error_text=str(settlement_errors),
        sender_contention=True,
        evidence_files=[str(out_dir / "suite_two_node_accept.json")],
    )
    limitation["primary"] = "architecture"
    limitation["layer"] = "architecture"

    result = {
        "suite": "E_two_node_accept",
        "elapsed_s": sw.elapsed(),
        "attempts": attempts,
        "workers": workers,
        "amount": amount,
        "seed_balance": seed_balance,
        "pre_settled": pre_settled,
        "accepted": accepted,
        "rejected": rejected,
        "settled": settled,
        "per_node": per_node,
        "latency": latency.summary(),
        "source_balance": str(balance) if balance is not None else None,
        "expected_balance": str(expected),
        "negative_balance": bool(balance is not None and balance < 0),
        "peer_settlement_status": peer_settlement_status,
        "peer_tip_matches_root": peer_tip_matches,
        "settlement_errors": settlement_errors,
        "ok": correctness_ok,
        "limitations": [limitation],
    }
    write_json(out_dir / "suite_two_node_accept.json", result)
    return result
