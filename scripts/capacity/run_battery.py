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
from lib_planes import sample_planes, wallet_count  # noqa: E402
from suite_contention import run_suite_contention  # noqa: E402
from suite_money import run_suite_money  # noqa: E402
from suite_populate_accounts import run_suite_populate_accounts  # noqa: E402
from suite_throughput import run_suite_throughput  # noqa: E402
from suite_two_node_accept import run_suite_two_node_accept  # noqa: E402
from suite_wallets import run_suite_wallets  # noqa: E402
from suite_index_health import run_suite_index_health  # noqa: E402

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
    p.add_argument("--skip-contention", action="store_true")
    p.add_argument("--skip-two-node-accept", action="store_true")
    p.add_argument("--skip-smoke", action="store_true", help="Skip post-check smoke-two-nodes.sh")
    p.add_argument("--money-volume-chunk", type=int, default=1_000)
    p.add_argument("--money-volume-chunks", type=int, default=1_000)
    p.add_argument("--wallet-time-budget-s", type=float, default=1200.0)
    p.add_argument("--wallet-hard-cap", type=int, default=100_000)
    p.add_argument("--wallet-workers", type=int, default=64)
    p.add_argument("--throughput-duration-s", type=float, default=180.0)
    p.add_argument("--throughput-pairs", type=int, default=200)
    p.add_argument("--throughput-workers", type=int, default=128)
    p.add_argument(
        "--mine-every-sweep",
        default="500",
        help="Comma-separated settlement trigger sizes, for example 100,500,1000,2000",
    )
    p.add_argument("--contention-attempts", type=int, default=1_000)
    p.add_argument("--contention-workers", type=int, default=128)
    p.add_argument("--peer-url", default="http://localhost:8081")
    p.add_argument("--two-node-attempts", type=int, default=1_000)
    p.add_argument("--two-node-workers", type=int, default=128)
    p.add_argument(
        "--populate-accounts",
        type=int,
        default=0,
        help="Bulk-insert synthetic wallets before suites (0=skip)",
    )
    p.add_argument(
        "--require-wallet-count",
        type=int,
        default=0,
        help="Abort if ledger_wallet count is below N before behavioral suites",
    )
    p.add_argument(
        "--run-index-health",
        action="store_true",
        help="Run PostgreSQL EXPLAIN gate after populate / before throughput",
    )
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

    if args.populate_accounts > 0:
        print(f"==> Bulk populate accounts → {args.populate_accounts}")
        pop = run_suite_populate_accounts(out_dir, target_count=args.populate_accounts)
        summary["suites"]["populate_accounts"] = {
            "ok": pop.get("ok"),
            "after_count": pop.get("after_count"),
            "rows_per_s": pop.get("rows_per_s"),
        }
        print(f"    after_count={pop.get('after_count')} rows/s={pop.get('rows_per_s')}")

    if args.require_wallet_count > 0:
        have = wallet_count()
        if have < args.require_wallet_count:
            raise SystemExit(
                f"require-wallet-count={args.require_wallet_count} but ledger has {have}"
            )
        print(f"==> require-wallet-count satisfied ({have})")

    if args.run_index_health:
        print("==> Index health gate")
        idx = run_suite_index_health(out_dir)
        summary["suites"]["index_health"] = {
            "ok": idx.get("ok"),
            "failures": idx.get("failures"),
        }
        limitations.extend(idx.get("limitations") or [])
        print(f"    ok={idx.get('ok')}")

    with ApiClient(args.base_url, timeout_s=120.0) as api:
        summary["planes_before"] = sample_planes(api)
        if not args.skip_money:
            print("==> Suite A — money ceiling")
            money = run_suite_money(
                api,
                out_dir,
                args.mongo_uri,
                volume_chunk=args.money_volume_chunk,
                volume_chunks=args.money_volume_chunks,
            )
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
            mine_sweep = [
                int(value.strip())
                for value in args.mine_every_sweep.split(",")
                if value.strip()
            ]
            if not mine_sweep or any(value <= 0 for value in mine_sweep):
                raise SystemExit("--mine-every-sweep requires positive integers")
            for mine_every in mine_sweep:
                print(f"==> Suite C — host throughput (mine_every={mine_every})")
                evidence_file = (
                    "suite_throughput.json"
                    if len(mine_sweep) == 1
                    else f"suite_throughput_{mine_every}.json"
                )
                tp = run_suite_throughput(
                    api,
                    out_dir,
                    args.mongo_uri,
                    duration_s=args.throughput_duration_s,
                    wallet_pairs=args.throughput_pairs,
                    workers=args.throughput_workers,
                    mine_every=mine_every,
                    evidence_file=evidence_file,
                )
                suite_key = (
                    "C_throughput"
                    if len(mine_sweep) == 1
                    else f"C_throughput_mine_{mine_every}"
                )
                summary["suites"][suite_key] = {
                    "tx_per_s": tp.get("tx_per_s"),
                    "dollars_per_s": tp.get("dollars_per_s"),
                    "mine_per_s": tp.get("mine_per_s"),
                    "stop_reason": tp.get("stop_reason"),
                }
                limitations.extend(tp.get("limitations") or [])
                print(
                    f"    tx/s={tp.get('tx_per_s'):.3f} "
                    f"$/s={tp.get('dollars_per_s'):.3f} "
                    f"stop={tp.get('stop_reason')}"
                )

        if not args.skip_contention:
            print("==> Suite D — same-wallet contention")
            contention = run_suite_contention(
                api,
                out_dir,
                args.mongo_uri,
                attempts=args.contention_attempts,
                workers=args.contention_workers,
            )
            summary["suites"]["D_contention"] = {
                "ok": contention.get("ok"),
                "accepted": contention.get("accepted"),
                "settled": contention.get("settled"),
                "negative_balance": contention.get("negative_balance"),
            }
            limitations.extend(contention.get("limitations") or [])

        if not args.skip_two_node_accept:
            print("==> Suite E — two-node concurrent accepts")
            two_node = run_suite_two_node_accept(
                api,
                args.peer_url,
                out_dir,
                args.mongo_uri,
                attempts=args.two_node_attempts,
                workers=args.two_node_workers,
            )
            summary["suites"]["E_two_node_accept"] = {
                "ok": two_node.get("ok"),
                "accepted": two_node.get("accepted"),
                "settled": two_node.get("settled"),
                "peer_tip_matches_root": two_node.get("peer_tip_matches_root"),
            }
            limitations.extend(two_node.get("limitations") or [])

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
    try:
        with ApiClient(args.base_url, timeout_s=30.0) as api:
            summary["planes_after"] = sample_planes(api)
    except Exception as exc:  # noqa: BLE001
        summary["planes_after"] = {"error": str(exc)}
    write_json(out_dir / "limitations.json", {"run_id": run_id, "items": limitations})
    write_json(out_dir / "summary.json", summary)

    print(f"==> Done. Summary → {out_dir / 'summary.json'}")
    print(f"    Limitations → {out_dir / 'limitations.json'}")
    print("    Fill docs/capacity/CAPACITY_BASELINE.md and LIMITATIONS.md from this evidence.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
