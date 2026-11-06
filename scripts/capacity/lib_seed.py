"""Seed funded wallets via CapacityWalletSeeder (WalletCodec-compatible)."""

from __future__ import annotations

import json
import os
import subprocess
from pathlib import Path
from typing import Any


def api_root() -> Path:
    return Path(__file__).resolve().parents[2]


def seed_funded_wallets(
    count: int,
    balance: int | str,
    mongo_uri: str | None = None,
    timeout_s: float = 300.0,
) -> list[dict[str, Any]]:
    """Insert ``count`` funded wallets; returns [{uid, address, balance}, ...]."""
    root = api_root()
    env = os.environ.copy()
    uri = mongo_uri or env.get("MONGO_URI", "mongodb://localhost:27017")
    cmd = [
        str(root / "mvnw"),
        "-q",
        "exec:java",
        "-Dexec.classpathScope=test",
        "-Dexec.mainClass=api.network.CapacityWalletSeeder",
        f"-Dquarkus.mongodb.connection-string={uri}",
        f"-Dcapacity.count={count}",
        f"-Dcapacity.balance={balance}",
    ]
    proc = subprocess.run(
        cmd,
        cwd=root,
        capture_output=True,
        text=True,
        timeout=timeout_s,
        check=False,
        env=env,
    )
    if proc.returncode != 0:
        raise RuntimeError(
            "CapacityWalletSeeder failed "
            f"(exit={proc.returncode}):\nstdout={proc.stdout}\nstderr={proc.stderr}"
        )
    line = proc.stdout.strip().splitlines()[-1] if proc.stdout.strip() else ""
    try:
        data = json.loads(line)
    except json.JSONDecodeError as exc:
        raise RuntimeError(f"Bad seeder JSON: {line!r}\nstderr={proc.stderr}") from exc
    if not isinstance(data, list):
        raise RuntimeError(f"Expected JSON array from seeder, got: {data!r}")
    return data
