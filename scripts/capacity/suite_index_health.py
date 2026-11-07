"""EXPLAIN gate for hot ledger lookups under volume."""

from __future__ import annotations

import os
import subprocess
from pathlib import Path
from typing import Any

from lib_classify import classify_failure
from lib_metrics import write_json
from lib_planes import sample_postgres, wallet_count


def _psql(sql: str) -> str:
    container = os.environ.get("POSTGRES_CONTAINER", "java-blockchain-api-postgres-1")
    user = os.environ.get("POSTGRES_USER", "blockchain")
    database = os.environ.get("POSTGRES_DB", "blockchain")
    proc = subprocess.run(
        [
            "docker",
            "exec",
            "-i",
            container,
            "psql",
            "-U",
            user,
            "-d",
            database,
            "-v",
            "ON_ERROR_STOP=1",
            "-c",
            sql,
        ],
        capture_output=True,
        text=True,
        check=False,
        timeout=120,
    )
    if proc.returncode != 0:
        raise RuntimeError(proc.stderr[-4000:] or proc.stdout[-4000:])
    return proc.stdout


def _plan_uses_seq_scan(plan: str) -> bool:
    lowered = plan.lower()
    # Fail only when a sequential scan appears without an index scan alternative
    # on the same plan root for our keyed lookups.
    return "seq scan" in lowered and "index" not in lowered


def run_suite_index_health(
    out_dir: Path,
    *,
    seq_scan_fail_above_rows: int = 10_000,
) -> dict[str, Any]:
    rows = wallet_count()
    sample = _psql(
        "SELECT uid_hash, public_key FROM ledger_wallet LIMIT 1;"
    )
    uid_hash = None
    public_key = None
    for line in sample.splitlines():
        if "|" in line and "uid_hash" not in line and "---" not in line:
            parts = [p.strip() for p in line.split("|")]
            if len(parts) >= 2 and parts[0] and parts[0] != "uid_hash":
                uid_hash, public_key = parts[0], parts[1]
                break

    plans: dict[str, str] = {}
    failures: list[str] = []
    if uid_hash and public_key:
        pk_literal = public_key.replace("'", "''")
        probes = {
            "wallet_by_uid_hash": (
                f"EXPLAIN (ANALYZE, BUFFERS) SELECT * FROM ledger_wallet "
                f"WHERE uid_hash = '{uid_hash}';"
            ),
            "wallet_by_public_key": (
                f"EXPLAIN (ANALYZE, BUFFERS) SELECT * FROM ledger_wallet "
                f"WHERE public_key = '{pk_literal}';"
            ),
            "pending_by_created": (
                "EXPLAIN (ANALYZE, BUFFERS) SELECT * FROM ledger_pending_transaction "
                "ORDER BY created_at, transaction_id LIMIT 500;"
            ),
            "settled_by_tx": (
                "EXPLAIN (ANALYZE, BUFFERS) SELECT * FROM ledger_settled_transaction "
                "WHERE transaction_id = '00000000-0000-0000-0000-000000000001';"
            ),
        }
        keyed = {"wallet_by_uid_hash", "wallet_by_public_key", "settled_by_tx"}
        for name, sql in probes.items():
            plan = _psql(sql)
            plans[name] = plan
            if (
                name in keyed
                and rows >= seq_scan_fail_above_rows
                and _plan_uses_seq_scan(plan)
            ):
                failures.append(name)
    else:
        failures.append("no_wallet_sample")

    ok = not failures
    limitation = None
    if not ok:
        limitation = classify_failure(
            suite="index_health",
            metric="hot_lookup_plan",
            last_good=None,
            first_fail=failures[0] if failures else "plan",
            signal=f"seq_scan_or_missing on {failures}; wallet_rows={rows}",
            evidence_files=[str(out_dir / "suite_index_health.json")],
        )
        limitation["primary"] = "infrastructure"
        limitation["layer"] = "infrastructure"
        limitation["secondary"] = ["architecture"]

    result = {
        "suite": "index_health",
        "ok": ok,
        "wallet_count": rows,
        "seq_scan_fail_above_rows": seq_scan_fail_above_rows,
        "failures": failures,
        "plans": plans,
        "postgres": sample_postgres(),
        "limitations": [limitation] if limitation else [],
    }
    write_json(out_dir / "suite_index_health.json", result)
    return result
