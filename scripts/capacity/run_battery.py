#!/usr/bin/env python3
"""JBlock API Phase-1 capacity battery orchestrator."""

from __future__ import annotations

import argparse
import subprocess
import sys
import time
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))

from lib_classify import architecture_notes_from_design, classify_failure  # noqa: E402
from lib_http import ApiClient  # noqa: E402
from lib_metrics import machine_fingerprint, write_json  # noqa: E402
from suite_money import run_suite_money  # noqa: E402
from suite_throughput import run_suite_throughput  # noqa: E402
from suite_wallets import run_suite_wallets  # noqa: E402

API_ROOT = HERE.parents[1]
DOCS_EVIDENCE = API_ROOT / "docs" / "capacity" / "evidence"


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(description="Run JBlock API local capacity battery (Phase 1)")
    p.add_argument("--base-url", default="http://localhost:8018")
    p.add_argument("--mongo-uri", default="mongodb://localhost:27017")
    p.add_argument("--run-id", default=None, help="Evidence directory name (default: UTC timestamp)")
    p.add_argument("--skip-money", action="store_true")
    p.add_argument("--skip-wallets", action="store_true")
    p.add_argument("--skip-throughput", action="store_true")
    p.add_argument("--skip-smoke", action="store_true", help="Skip post-check smoke-two-nodes.sh")
    p.add_argument("--wallet-time-budget-s", type=float, default=600.0)
    p.add_argument("--wallet-hard-cap", type=int, default=5000)
    p.add_argument("--wallet-workers", type=int, default=32)
    p.add_argument("--throughput-duration-s", type=float, default=90.0)
    p.add_argument("--throughput-pairs", type=int, default=20)
    p.add_argument("--throughput-workers", type=int, default=32)
    return p.parse_args()


def wait_ready(url: str, timeout_s: float = 120.0) -> None:
    deadline = time.time() + timeout_s
    with ApiClient(url) as api:
        while time.time() < deadline:
            if api.ready().ok:
                return
            time.sleep(2)
    raise SystemExit(f"API not ready at {url}/q/health/ready within {timeout_s}s")


def run_smoke_postcheck(out_dir: Path) -> dict:
    script = API_ROOT / "scripts" / "smoke-two-nodes.sh"
    if not script.exists():
        return {"ok": False, "error": "smoke-two-nodes.sh missing"}
    proc = subprocess.run(
        ["bash", str(script)],
        cwd=API_ROOT,
        capture_output=True,
        text=True,
        timeout=300,
        check=False,
    )
    payload = {
        "ok": proc.returncode == 0,
        "returncode": proc.returncode,
        "stdout_tail": proc.stdout[-4000:],
        "stderr_tail": proc.stderr[-2000:],
    }
    write_json(out_dir / "smoke_two_nodes.json", payload)
    return payload


def main() -> int:
    args = parse_args()
    run_id = args.run_id or time.strftime("%Y%m%dT%H%M%SZ", time.gmtime())
    out_dir = DOCS_EVIDENCE / run_id
    out_dir.mkdir(parents=True, exist_ok=True)

    print(f"==> Capacity battery run_id={run_id}")
    print(f"    evidence → {out_dir}")
    print(f"    base_url={args.base_url} mongo={args.mongo_uri}")

    wait_ready(args.base_url)
    write_json(out_dir / "machine.json", machine_fingerprint())

    limitations: list = list(architecture_notes_from_design())
    summary: dict = {"run_id": run_id, "base_url": args.base_url, "suites": {}}

    with ApiClient(args.base_url, timeout_s=120.0) as api:
        if not args.skip_money:
            print("==> Suite A — money ceiling")
            money = run_suite_money(api, out_dir, args.mongo_uri)
            summary["suites"]["A_money"] = {
                "last_good_single_amount": money.get("last_good_single_amount"),
                "first_fail_single_amount": money.get("first_fail_single_amount"),
                "settled_volume": (money.get("settled_volume") or {}).get("settled_volume"),
            }
            limitations.extend(money.get("limitations") or [])
            print(
                f"    last_good={money.get('last_good_single_amount')} "
                f"first_fail={money.get('first_fail_single_amount')}"
            )

        if not args.skip_wallets:
            print("==> Suite B — wallet ceiling")
            wallets = run_suite_wallets(
                api,
                out_dir,
                time_budget_s=args.wallet_time_budget_s,
                hard_cap=args.wallet_hard_cap,
                workers=args.wallet_workers,
            )
            summary["suites"]["B_wallets"] = {
                "last_good_wallet_count": wallets.get("last_good_wallet_count"),
                "first_fail_wallet_count": wallets.get("first_fail_wallet_count"),
                "stop_reason": wallets.get("stop_reason"),
            }
            limitations.extend(wallets.get("limitations") or [])
            print(
                f"    last_good={wallets.get('last_good_wallet_count')} "
                f"stop={wallets.get('stop_reason')}"
            )

        if not args.skip_throughput:
            print("==> Suite C — host throughput")
            tp = run_suite_throughput(
                api,
                out_dir,
                args.mongo_uri,
                duration_s=args.throughput_duration_s,
                wallet_pairs=args.throughput_pairs,
                workers=args.throughput_workers,
            )
            summary["suites"]["C_throughput"] = {
                "tx_per_s": tp.get("tx_per_s"),
                "dollars_per_s": tp.get("dollars_per_s"),
                "mine_per_s": tp.get("mine_per_s"),
                "stop_reason": tp.get("stop_reason"),
            }
            limitations.extend(tp.get("limitations") or [])
            print(
                f"    tx/s={tp.get('tx_per_s'):.3f} $/s={tp.get('dollars_per_s'):.3f} "
                f"stop={tp.get('stop_reason')}"
            )

    smoke = None
    if not args.skip_smoke:
        print("==> Post-check smoke-two-nodes.sh (comfortable load)")
        try:
            smoke = run_smoke_postcheck(out_dir)
            print(f"    smoke ok={smoke.get('ok')}")
            if not smoke.get("ok"):
                limitations.append(
                    classify_failure(
                        suite="smoke_two_nodes",
                        metric="peer_sync",
                        last_good=None,
                        first_fail="sync_failed",
                        signal="multi-node smoke failed after capacity suites",
                        error_text=smoke.get("stderr_tail") or smoke.get("stdout_tail"),
                        evidence_files=[str(out_dir / "smoke_two_nodes.json")],
                    )
                )
        except Exception as exc:  # noqa: BLE001
            smoke = {"ok": False, "error": str(exc)}
            write_json(out_dir / "smoke_two_nodes.json", smoke)
            print(f"    smoke error: {exc}")

    summary["smoke_two_nodes"] = smoke
    write_json(out_dir / "limitations.json", {"run_id": run_id, "items": limitations})
    write_json(out_dir / "summary.json", summary)

    print(f"==> Done. Summary → {out_dir / 'summary.json'}")
    print(f"    Limitations → {out_dir / 'limitations.json'}")
    print("    Fill docs/capacity/CAPACITY_BASELINE.md and LIMITATIONS.md from this evidence.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
