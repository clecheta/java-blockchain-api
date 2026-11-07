#!/usr/bin/env python3
"""Run the V5 account-volume stage to completion on the local machine.

Default targets: 5,000,000 accounts present and 150,000,000,000 units settled.
Duration and host saturation are not stop conditions.
"""

from __future__ import annotations

import argparse
import sys
import time
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))

from lib_http import ApiClient  # noqa: E402
from lib_metrics import machine_fingerprint, write_json  # noqa: E402
from suite_volume_stage import (  # noqa: E402
    DEFAULT_ACCOUNTS,
    DEFAULT_MINE_EVERY,
    DEFAULT_MOVED,
    DEFAULT_PAIRS,
    DEFAULT_TX_AMOUNT,
    DEFAULT_WORKERS,
    run_suite_volume_stage,
)

API_ROOT = HERE.parents[1]
DOCS_EVIDENCE = API_ROOT / "docs" / "capacity" / "evidence"


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(
        description=(
            "Run V5 volume stage: keep N accounts present and settle a moved "
            "total. Stops only on target_reached or correctness_failed."
        )
    )
    p.add_argument("--base-url", default="http://localhost:8018")
    p.add_argument("--run-id", default=None)
    p.add_argument("--accounts", type=int, default=DEFAULT_ACCOUNTS)
    p.add_argument("--moved", type=int, default=DEFAULT_MOVED)
    p.add_argument("--tx-amount", type=int, default=DEFAULT_TX_AMOUNT)
    p.add_argument("--pairs", type=int, default=DEFAULT_PAIRS)
    p.add_argument("--workers", type=int, default=DEFAULT_WORKERS)
    p.add_argument("--mine-every", type=int, default=DEFAULT_MINE_EVERY)
    return p.parse_args()


def wait_ready(url: str, timeout_s: float = 180.0) -> None:
    deadline = time.time() + timeout_s
    with ApiClient(url) as api:
        while time.time() < deadline:
            if api.ready().ok:
                return
            time.sleep(2)
    raise SystemExit(f"API not ready at {url}/q/health/ready within {timeout_s}s")


def main() -> int:
    args = parse_args()
    run_id = args.run_id or (
        "volume-v5-" + time.strftime("%Y%m%dT%H%M%SZ", time.gmtime())
    )
    out_dir = DOCS_EVIDENCE / run_id
    out_dir.mkdir(parents=True, exist_ok=True)
    write_json(out_dir / "machine.json", machine_fingerprint())

    print(f"==> Volume stage run_id={run_id}")
    print(f"    evidence → {out_dir}")
    print(
        f"    accounts={args.accounts} moved={args.moved} "
        f"tx_amount={args.tx_amount} pairs={args.pairs}"
    )
    print("    stop conditions: target_reached | correctness_failed")
    print("    time / host saturation are NOT stop conditions")

    wait_ready(args.base_url)
    with ApiClient(args.base_url, timeout_s=300.0) as api:
        result = run_suite_volume_stage(
            api,
            out_dir,
            accounts=args.accounts,
            moved=args.moved,
            tx_amount=args.tx_amount,
            pairs=args.pairs,
            workers=args.workers,
            mine_every=args.mine_every,
        )

    summary = {
        "run_id": run_id,
        "ok": result.get("ok"),
        "stop_reason": result.get("stop_reason"),
        "accounts_target": result.get("accounts_target"),
        "accounts_present": result.get("accounts_present"),
        "moved_target": result.get("moved_target"),
        "settled_volume": result.get("settled_volume"),
        "elapsed_s": result.get("elapsed_s"),
        "evidence": str(out_dir),
    }
    write_json(out_dir / "summary.json", summary)
    print(
        f"==> done ok={summary['ok']} stop={summary['stop_reason']} "
        f"settled={summary['settled_volume']} accounts={summary['accounts_present']}"
    )
    return 0 if result.get("ok") else 1


if __name__ == "__main__":
    raise SystemExit(main())
