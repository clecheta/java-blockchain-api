"""Backup/restore drill and restart/boot budget for volume diagnosis."""

from __future__ import annotations

import os
import signal
import subprocess
import time
from pathlib import Path
from typing import Any

from lib_http import ApiClient
from lib_metrics import Stopwatch, write_json
from lib_planes import sample_planes, wallet_count


API_ROOT = Path(__file__).resolve().parents[2]


def _run(cmd: list[str], *, timeout: float = 600) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        cmd,
        cwd=str(API_ROOT),
        capture_output=True,
        text=True,
        check=False,
        timeout=timeout,
    )


def run_suite_restore(
    out_dir: Path,
    *,
    expected_wallets: int | None = None,
) -> dict[str, Any]:
    sw = Stopwatch()
    dump = out_dir / "ledger-capacity.dump"
    backup = _run(["bash", "scripts/postgres/backup.sh", str(dump)])
    restore = _run(["bash", "scripts/postgres/restore-drill.sh", str(dump)])
    ok = backup.returncode == 0 and restore.returncode == 0
    observed = None
    if "wallet_count" in restore.stdout:
        for line in restore.stdout.splitlines():
            if line.strip().isdigit():
                observed = int(line.strip())
                break
    if expected_wallets is not None and observed is not None:
        ok = ok and observed >= expected_wallets
    result = {
        "suite": "restore_drill",
        "ok": ok,
        "elapsed_s": sw.elapsed(),
        "dump": str(dump),
        "expected_wallets": expected_wallets,
        "observed_wallet_count_hint": observed,
        "live_wallet_count": wallet_count(),
        "backup_returncode": backup.returncode,
        "restore_returncode": restore.returncode,
        "backup_stderr_tail": backup.stderr[-2000:],
        "restore_stdout_tail": restore.stdout[-4000:],
        "restore_stderr_tail": restore.stderr[-2000:],
    }
    write_json(out_dir / "suite_restore_drill.json", result)
    return result


def _pids_on_port(port: int) -> list[int]:
    proc = subprocess.run(
        ["ss", "-ltnp"],
        capture_output=True,
        text=True,
        check=False,
    )
    pids: list[int] = []
    needle = f":{port}"
    for line in proc.stdout.splitlines():
        if needle not in line or "pid=" not in line:
            continue
        for part in line.split("pid=")[1:]:
            digits = "".join(ch for ch in part if ch.isdigit())
            if digits:
                pids.append(int(digits))
    return sorted(set(pids))


def run_suite_boot_budget(
    out_dir: Path,
    *,
    base_url: str = "http://localhost:8018",
    peer_url: str = "http://localhost:8081",
    ready_budget_s: float = 120.0,
    restart: bool = False,
) -> dict[str, Any]:
    """Measure ready latency; optionally restart packaged root/peer jars."""
    sw = Stopwatch()
    notes: list[str] = []
    root_ready_s = None
    peer_ready_s = None
    ok = True

    if restart:
        jar = API_ROOT / "target/quarkus-app/quarkus-run.jar"
        if not jar.exists():
            result = {
                "suite": "boot_budget",
                "ok": False,
                "error": f"missing {jar}; package the API first",
                "elapsed_s": sw.elapsed(),
            }
            write_json(out_dir / "suite_boot_budget.json", result)
            return result
        for port in (8018, 8081):
            for pid in _pids_on_port(port):
                try:
                    os.kill(pid, signal.SIGTERM)
                except OSError as exc:
                    notes.append(f"kill {pid}: {exc}")
        time.sleep(3)
        env = os.environ.copy()
        env.setdefault("BANK_SERVICE_API_KEY", "local-development-only-change-me")
        env.setdefault("BANK_CLIENT_API_KEY", "local-client-only-change-me")
        env.setdefault("OTEL_SDK_DISABLED", "true")
        root_log = out_dir / "boot-root.log"
        peer_log = out_dir / "boot-peer.log"
        with root_log.open("w", encoding="utf-8") as fh:
            subprocess.Popen(
                ["java", "-jar", str(jar)],
                cwd=str(API_ROOT),
                env=env,
                stdout=fh,
                stderr=subprocess.STDOUT,
                start_new_session=True,
            )
        time.sleep(2)
        with peer_log.open("w", encoding="utf-8") as fh:
            subprocess.Popen(
                ["java", "-Dquarkus.profile=node", "-jar", str(jar)],
                cwd=str(API_ROOT),
                env=env,
                stdout=fh,
                stderr=subprocess.STDOUT,
                start_new_session=True,
            )
        notes.append("restarted packaged root+peer")

    def _wait(url: str) -> float | None:
        deadline = time.time() + ready_budget_s
        t0 = time.perf_counter()
        with ApiClient(url, timeout_s=10.0) as api:
            while time.time() < deadline:
                if api.ready().ok:
                    return time.perf_counter() - t0
                time.sleep(1)
        return None

    root_ready_s = _wait(base_url)
    peer_ready_s = _wait(peer_url)
    ok = root_ready_s is not None and peer_ready_s is not None
    planes = None
    if ok:
        with ApiClient(base_url, timeout_s=30.0) as api:
            planes = sample_planes(api)

    result = {
        "suite": "boot_budget",
        "ok": ok,
        "restart": restart,
        "ready_budget_s": ready_budget_s,
        "root_ready_s": root_ready_s,
        "peer_ready_s": peer_ready_s,
        "elapsed_s": sw.elapsed(),
        "notes": notes,
        "planes": planes,
        "layer_hint": (
            None
            if ok
            else "architecture+source_code (boot/load path), not Kafka fan-out"
        ),
    }
    write_json(out_dir / "suite_boot_budget.json", result)
    return result
