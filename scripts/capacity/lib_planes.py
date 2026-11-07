"""Capture app / PostgreSQL / Kafka / host signals for failure-layer diagnosis."""

from __future__ import annotations

import os
import re
import subprocess
import time
from typing import Any

from lib_http import ApiClient
from lib_metrics import sample_host


def _docker_psql(sql: str) -> str:
    container = os.environ.get("POSTGRES_CONTAINER", "java-blockchain-api-postgres-1")
    user = os.environ.get("POSTGRES_USER", "blockchain")
    database = os.environ.get("POSTGRES_DB", "blockchain")
    proc = subprocess.run(
        [
            "docker",
            "exec",
            container,
            "psql",
            "-U",
            user,
            "-d",
            database,
            "-v",
            "ON_ERROR_STOP=1",
            "-t",
            "-A",
            "-c",
            sql,
        ],
        capture_output=True,
        text=True,
        check=False,
        timeout=60,
    )
    if proc.returncode != 0:
        raise RuntimeError(
            f"psql failed ({proc.returncode}): {proc.stderr.strip() or proc.stdout.strip()}"
        )
    return proc.stdout.strip()


def wallet_count() -> int:
    return int(_docker_psql("SELECT count(*) FROM ledger_wallet;"))


def sample_postgres() -> dict[str, Any]:
    out: dict[str, Any] = {"ts": time.time()}
    try:
        out["wallet_count"] = wallet_count()
        out["pending_count"] = int(
            _docker_psql("SELECT count(*) FROM ledger_pending_transaction;")
        )
        out["block_count"] = int(_docker_psql("SELECT count(*) FROM ledger_block;"))
        out["ledger_wallet_bytes"] = int(
            _docker_psql("SELECT pg_total_relation_size('ledger_wallet');")
        )
        out["active_connections"] = int(
            _docker_psql("SELECT count(*) FROM pg_stat_activity WHERE datname = current_database();")
        )
        waits = _docker_psql(
            "SELECT coalesce(wait_event_type, 'none') || ':' || "
            "coalesce(wait_event, 'none') || '=' || count(*)::text "
            "FROM pg_stat_activity WHERE datname = current_database() "
            "GROUP BY wait_event_type, wait_event "
            "ORDER BY count(*) DESC LIMIT 8;"
        )
        out["wait_events"] = [line for line in waits.splitlines() if line]
        locks = _docker_psql(
            "SELECT count(*) FROM pg_locks l "
            "JOIN pg_class c ON c.oid = l.relation "
            "WHERE c.relname = 'ledger_wallet' AND NOT l.granted;"
        )
        out["ledger_wallet_ungranted_locks"] = int(locks or "0")
    except Exception as exc:  # noqa: BLE001
        out["error"] = f"{type(exc).__name__}: {exc}"
    return out


def _parse_prometheus_value(metrics_text: str, metric_name: str) -> float | None:
    pattern = re.compile(
        rf"^{re.escape(metric_name)}(?:\{{[^}}]*\}})?\s+([0-9.eE+-]+)\s*$",
        re.MULTILINE,
    )
    match = pattern.search(metrics_text)
    if not match:
        return None
    try:
        return float(match.group(1))
    except ValueError:
        return None


def sample_app(api: ApiClient) -> dict[str, Any]:
    out: dict[str, Any] = {"ts": time.time(), "base_url": api.base_url}
    ready = api.ready()
    out["ready_ok"] = ready.ok
    out["ready_status"] = ready.status
    metrics = api._request("GET", "/q/metrics")  # noqa: SLF001 — shared client
    out["metrics_ok"] = metrics.ok
    if not metrics.ok or not isinstance(metrics.text, str):
        out["error"] = metrics.error
        return out
    text = metrics.text
    out["jblock_transfer_accept_count"] = _parse_prometheus_value(
        text, "jblock_transfer_accept_seconds_count"
    )
    out["jblock_settlement_count"] = _parse_prometheus_value(
        text, "jblock_settlement_seconds_count"
    )
    out["agroal_active_count"] = _parse_prometheus_value(
        text, "agroal_active_count"
    ) or _parse_prometheus_value(text, "vendor_agroal_active_count")
    out["agroal_waiting_count"] = _parse_prometheus_value(
        text, "agroal_awaiting_count"
    ) or _parse_prometheus_value(text, "vendor_agroal_awaiting_count")
    return out


def sample_kafka() -> dict[str, Any]:
    """Best-effort local compose Kafka signals via docker logs / kafka topics."""
    out: dict[str, Any] = {"ts": time.time(), "role": "fan_out_not_sot"}
    container = os.environ.get("KAFKA_CONTAINER", "java-blockchain-api-kafka-1")
    try:
        proc = subprocess.run(
            [
                "docker",
                "exec",
                container,
                "bash",
                "-lc",
                "/opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list 2>/dev/null "
                "|| /bin/kafka-topics --bootstrap-server localhost:9092 --list 2>/dev/null "
                "|| true",
            ],
            capture_output=True,
            text=True,
            check=False,
            timeout=30,
        )
        topics = [line.strip() for line in proc.stdout.splitlines() if line.strip()]
        out["topics"] = topics
        out["topics_ok"] = proc.returncode == 0 and bool(topics)
        if proc.returncode != 0 and not topics:
            out["note"] = "topic list unavailable; treat Kafka as opaque fan-out"
    except Exception as exc:  # noqa: BLE001
        out["error"] = f"{type(exc).__name__}: {exc}"
    return out


def sample_planes(api: ApiClient | None = None) -> dict[str, Any]:
    planes: dict[str, Any] = {
        "captured_at": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
        "host": sample_host(),
        "postgres": sample_postgres(),
        "kafka": sample_kafka(),
    }
    if api is not None:
        planes["app"] = sample_app(api)
    return planes
