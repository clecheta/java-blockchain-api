"""Bulk-populate ledger_wallet for fat-PostgreSQL volume diagnosis."""

from __future__ import annotations

import os
import subprocess
import time
from pathlib import Path
from typing import Any

from lib_metrics import Stopwatch, write_json
from lib_planes import sample_postgres, wallet_count


def run_suite_populate_accounts(
    out_dir: Path,
    *,
    target_count: int,
    batch_size: int = 50_000,
    prefix: str = "bulk",
) -> dict[str, Any]:
    if target_count <= 0:
        raise ValueError("target_count must be positive")
    sw = Stopwatch()
    before = wallet_count()
    remaining = max(0, target_count - before)
    inserted = 0
    batches: list[dict[str, Any]] = []

    while inserted < remaining:
        n = min(batch_size, remaining - inserted)
        start = before + inserted
        # Synthetic keys: fat-table volume only. Behavioral suites still create
        # real ECDSA wallets via HTTP for signing paths.
        sql = f"""
SET synchronous_commit = off;
INSERT INTO ledger_wallet (
  uid_hash, public_key, private_key, balance, starting_balance,
  reserved_balance, nonce, created_at, updated_at
)
SELECT
  md5('{prefix}-' || g::text),
  'bulk-pub-' || '{prefix}-' || g::text,
  '00',
  0,
  0,
  0,
  0,
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
FROM generate_series({start}, {start + n - 1}) AS g
ON CONFLICT (uid_hash) DO NOTHING;
"""
        t0 = time.perf_counter()
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
            ],
            input=sql,
            capture_output=True,
            text=True,
            check=False,
            timeout=600,
        )
        elapsed = time.perf_counter() - t0
        if proc.returncode != 0:
            raise RuntimeError(proc.stderr[-4000:] or proc.stdout[-4000:])
        batches.append({"start": start, "n": n, "elapsed_s": elapsed})
        inserted += n

    after = wallet_count()
    pg = sample_postgres()
    result = {
        "suite": "populate_accounts",
        "ok": after >= target_count,
        "target_count": target_count,
        "before_count": before,
        "after_count": after,
        "inserted_requested": remaining,
        "elapsed_s": sw.elapsed(),
        "rows_per_s": (after - before) / sw.elapsed() if sw.elapsed() > 0 else None,
        "batches": batches,
        "postgres": pg,
        "note": (
            "Synthetic bulk rows for PostgreSQL volume pressure; not API-created "
            "ECDSA wallets. Use Suite B for real create-path measurement."
        ),
    }
    write_json(out_dir / "suite_populate_accounts.json", result)
    return result
