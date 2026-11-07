#!/usr/bin/env python3
"""Isolation matrix: classify volume cliffs as host / PostgreSQL / Kafka / code / architecture."""

from __future__ import annotations

import argparse
import sys
import time
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))

from lib_http import ApiClient  # noqa: E402
from lib_metrics import machine_fingerprint, write_json  # noqa: E402
from lib_planes import sample_planes, wallet_count  # noqa: E402
from suite_index_health import run_suite_index_health  # noqa: E402
from suite_populate_accounts import run_suite_populate_accounts  # noqa: E402
from suite_restore_boot import run_suite_boot_budget, run_suite_restore  # noqa: E402
from suite_contention import run_suite_contention  # noqa: E402
from suite_throughput import run_suite_throughput  # noqa: E402

API_ROOT = HERE.parents[1]
DOCS_EVIDENCE = API_ROOT / "docs" / "capacity" / "evidence"

DECISION_RULES = [
    {
        "id": "host_infrastructure",
        "if": "Cliff vanishes on a larger host with same PG schema/code",
        "primary_layer": "host_infrastructure",
    },
    {
        "id": "postgresql",
        "if": "Cliff worsens only as ledger_wallet/indexes grow; EXPLAIN seq scans or lock waits spike",
        "primary_layer": "postgresql_infrastructure",
    },
    {
        "id": "source_code",
        "if": "Cliff persists on empty/small DB with same workers; CPU in ECDSA/JSON/Hibernate",
        "primary_layer": "source_code",
    },
    {
        "id": "architecture",
        "if": "Correctness breaks (overspend, duplicate settle, lost pending) even when TPS is fine",
        "primary_layer": "architecture",
    },
    {
        "id": "kafka",
        "if": "Accept TPS fine with skip-transaction-broadcast; collapses with fan-out on",
        "primary_layer": "kafka_infrastructure",
    },
    {
        "id": "boot_path",
        "if": "Boot/restart hangs validating full chain after volume",
        "primary_layer": "architecture+source_code",
    },
]


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(
        description="Run volume failure-layer isolation matrix (Phase-1 diagnostic)"
    )
    p.add_argument("--base-url", default="http://localhost:8018")
    p.add_argument("--peer-url", default="http://localhost:8081")
    p.add_argument("--run-id", default=None)
    p.add_argument(
        "--populate-accounts",
        type=int,
        default=50_000,
        help="Bulk synthetic wallet target for fat-PG pressure (default 50000)",
    )
    p.add_argument("--skip-populate", action="store_true")
    p.add_argument("--skip-index", action="store_true")
    p.add_argument("--skip-db-pressure", action="store_true")
    p.add_argument("--skip-throughput", action="store_true")
    p.add_argument("--skip-restore", action="store_true")
    p.add_argument("--skip-boot", action="store_true")
    p.add_argument(
        "--boot-restart",
        action="store_true",
        help="Stop/start packaged jars during boot budget (destructive)",
    )
    p.add_argument("--throughput-duration-s", type=float, default=45.0)
    p.add_argument("--throughput-pairs", type=int, default=40)
    p.add_argument("--throughput-workers", type=int, default=64)
    p.add_argument(
        "--kafka-quiet",
        action="store_true",
        help="Record kafka-quiet cell; requires API started with "
        "-Dledger.diagnostic.skip-transaction-broadcast=true",
    )
    return p.parse_args()


def main() -> int:
    args = parse_args()
    run_id = args.run_id or (
        "isolation-" + time.strftime("%Y%m%dT%H%M%SZ", time.gmtime())
    )
    out_dir = DOCS_EVIDENCE / run_id
    out_dir.mkdir(parents=True, exist_ok=True)
    print(f"==> Isolation matrix run_id={run_id}")
    print(f"    evidence → {out_dir}")

    summary: dict = {
        "run_id": run_id,
        "decision_rules": DECISION_RULES,
        "cells": {},
        "layer_calls": [],
    }
    write_json(out_dir / "machine.json", machine_fingerprint())

    with ApiClient(args.base_url, timeout_s=120.0) as api:
        summary["planes_before"] = sample_planes(api)

        if not args.skip_populate:
            print(f"==> Populate synthetic accounts → {args.populate_accounts}")
            pop = run_suite_populate_accounts(
                out_dir, target_count=args.populate_accounts
            )
            summary["cells"]["populate"] = {
                "ok": pop.get("ok"),
                "after_count": pop.get("after_count"),
                "rows_per_s": pop.get("rows_per_s"),
            }
            print(
                f"    wallets={pop.get('after_count')} "
                f"rows/s={pop.get('rows_per_s')}"
            )

        if not args.skip_index:
            print("==> Index health (PostgreSQL plane)")
            idx = run_suite_index_health(out_dir)
            summary["cells"]["index_health"] = {
                "ok": idx.get("ok"),
                "failures": idx.get("failures"),
                "wallet_count": idx.get("wallet_count"),
            }
            if not idx.get("ok"):
                summary["layer_calls"].append(
                    {
                        "cell": "index_health",
                        "primary_layer": "postgresql_infrastructure",
                        "signal": idx.get("failures"),
                    }
                )
            print(f"    ok={idx.get('ok')} failures={idx.get('failures')}")

        if not args.skip_db_pressure:
            print("==> DB-pressure contention (Suite D on populated ledger)")
            contention = run_suite_contention(
                api,
                out_dir,
                attempts=500,
                workers=64,
            )
            # Write under matrix-specific name as well
            write_json(out_dir / "cell_db_pressure_contention.json", contention)
            summary["cells"]["db_pressure_contention"] = {
                "ok": contention.get("ok"),
                "accepted": contention.get("accepted"),
                "settled": contention.get("settled"),
                "negative_balance": contention.get("negative_balance"),
            }
            if contention.get("negative_balance") or (
                contention.get("accepted")
                and contention.get("settled") != contention.get("accepted")
                and contention.get("ok") is False
            ):
                summary["layer_calls"].append(
                    {
                        "cell": "db_pressure_contention",
                        "primary_layer": "architecture"
                        if contention.get("negative_balance")
                        else "postgresql_infrastructure",
                        "signal": {
                            "accepted": contention.get("accepted"),
                            "settled": contention.get("settled"),
                            "balance": contention.get("source_balance"),
                        },
                    }
                )
            print(
                f"    ok={contention.get('ok')} "
                f"accepted={contention.get('accepted')} "
                f"settled={contention.get('settled')}"
            )

        if not args.skip_throughput:
            label = "kafka_quiet_throughput" if args.kafka_quiet else "baseline_throughput"
            print(f"==> Throughput cell ({label})")
            evidence = (
                "cell_kafka_quiet_throughput.json"
                if args.kafka_quiet
                else "cell_baseline_throughput.json"
            )
            tp = run_suite_throughput(
                api,
                out_dir,
                duration_s=args.throughput_duration_s,
                wallet_pairs=args.throughput_pairs,
                workers=args.throughput_workers,
                mine_every=500,
                evidence_file=evidence,
            )
            summary["cells"][label] = {
                "ok": True,
                "tx_per_s": tp.get("tx_per_s"),
                "tx_ok": tp.get("tx_ok"),
                "tx_fail": tp.get("tx_fail"),
                "kafka_quiet": args.kafka_quiet,
                "stop_reason": tp.get("stop_reason"),
            }
            summary["planes_after_throughput"] = sample_planes(api)
            print(
                f"    tx/s={tp.get('tx_per_s')} "
                f"kafka_quiet={args.kafka_quiet}"
            )
            if args.kafka_quiet:
                summary["layer_calls"].append(
                    {
                        "cell": label,
                        "primary_layer": "compare_to_baseline_throughput",
                        "signal": (
                            "If tx/s rises materially vs baseline_throughput, "
                            "cliff includes kafka_infrastructure coupling"
                        ),
                        "tx_per_s": tp.get("tx_per_s"),
                    }
                )

    if not args.skip_restore:
        print("==> Restore drill")
        restore = run_suite_restore(
            out_dir, expected_wallets=wallet_count()
        )
        summary["cells"]["restore_drill"] = {
            "ok": restore.get("ok"),
            "elapsed_s": restore.get("elapsed_s"),
        }
        if not restore.get("ok"):
            summary["layer_calls"].append(
                {
                    "cell": "restore_drill",
                    "primary_layer": "postgresql_infrastructure",
                    "signal": "backup/restore failed",
                }
            )
        print(f"    ok={restore.get('ok')}")

    if not args.skip_boot:
        print("==> Boot budget")
        boot = run_suite_boot_budget(
            out_dir,
            base_url=args.base_url,
            peer_url=args.peer_url,
            restart=args.boot_restart,
        )
        summary["cells"]["boot_budget"] = {
            "ok": boot.get("ok"),
            "root_ready_s": boot.get("root_ready_s"),
            "peer_ready_s": boot.get("peer_ready_s"),
            "restart": boot.get("restart"),
        }
        if not boot.get("ok"):
            summary["layer_calls"].append(
                {
                    "cell": "boot_budget",
                    "primary_layer": "architecture+source_code",
                    "signal": "ready budget exceeded after volume",
                }
            )
        print(
            f"    ok={boot.get('ok')} "
            f"root_ready_s={boot.get('root_ready_s')}"
        )

    # Empty/small-DB hot-path instruction cell (operator compares separately)
    summary["cells"]["empty_db_hot_path"] = {
        "ok": None,
        "instruction": (
            "On a wiped ledger (or fresh compose volume), re-run: "
            f"run_battery.py --skip-money --skip-wallets --skip-contention "
            f"--skip-two-node-accept --throughput-duration-s "
            f"{args.throughput_duration_s} --throughput-pairs "
            f"{args.throughput_pairs}. If tx/s stays near the populated "
            "baseline, primary layer is source_code (ECDSA/HTTP/ORM), not PostgreSQL row count."
        ),
    }

    summary["how_to_read"] = (
        "Compare cells using decision_rules. Do not collapse host/PG/Kafka "
        "into a single 'infrastructure' bucket when assigning the next change."
    )
    write_json(out_dir / "isolation_summary.json", summary)
    print(f"==> Done. Summary → {out_dir / 'isolation_summary.json'}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
