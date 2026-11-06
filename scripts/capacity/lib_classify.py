"""Map observable capacity failures to architecture / source / infrastructure."""

from __future__ import annotations

from typing import Any


LAYERS = ("architecture", "source_code", "infrastructure")


def classify_failure(
    *,
    suite: str,
    metric: str,
    last_good: Any,
    first_fail: Any,
    signal: str,
    http_status: int | None = None,
    error_text: str | None = None,
    host_saturated: bool = False,
    ready_ok: bool = True,
    balance_mismatch: bool = False,
    amount_near_long_max: bool = False,
    kafka_hint: bool = False,
    sender_contention: bool = False,
    mine_required: bool = False,
    chain_latency_grows: bool = False,
    evidence_files: list[str] | None = None,
) -> dict[str, Any]:
    """Return a limitations.json entry with primary + secondary layers."""
    text = (error_text or "").lower()
    signal_l = (signal or "").lower()
    blob = f"{text} {signal_l}"

    primary = "infrastructure"
    secondary: list[str] = []

    if balance_mismatch and amount_near_long_max:
        primary = "source_code"
        secondary = ["infrastructure"]
    elif balance_mismatch:
        primary = "source_code"
    elif "concurrentmodification" in blob:
        primary = "source_code"
        secondary = ["architecture"]
    elif kafka_hint or "kafka" in blob or "serialization" in blob or "serdes" in blob:
        primary = "infrastructure"
        secondary = ["source_code"]
    elif not ready_ok or host_saturated or http_status in (0, 502, 503, 504):
        primary = "infrastructure"
    elif sender_contention or mine_required or chain_latency_grows:
        primary = "architecture"
        if chain_latency_grows:
            secondary = ["source_code"]
    elif http_status == 400 and ("wrong usage" in blob or "insufficient" in blob):
        primary = "architecture"
    elif http_status and http_status >= 500:
        primary = "infrastructure"
        secondary = ["source_code"]
    elif "timeout" in blob or "connect" in blob:
        primary = "infrastructure"
    else:
        # Default for unexplained cliffs under load
        primary = "infrastructure"
        secondary = ["architecture"]

    return {
        "layer": primary,
        "primary": primary,
        "secondary": secondary,
        "suite": suite,
        "metric": metric,
        "last_good": last_good,
        "first_fail": first_fail,
        "signal": signal,
        "http_status": http_status,
        "evidence_files": evidence_files or [],
    }


def architecture_notes_from_design() -> list[dict[str, Any]]:
    """Static architecture limits always present (measured or confirmed by suites)."""
    return [
        {
            "layer": "architecture",
            "primary": "architecture",
            "secondary": [],
            "suite": "design",
            "metric": "settlement_model",
            "last_good": None,
            "first_fail": None,
            "signal": "Settlement requires explicit mine; pool allows one pending tx per sender; "
            "wallet HTTP create starts at balance 0 (funding needs Mongo seed).",
            "evidence_files": [
                "src/main/java/api/cryptocurrency/PoolResource.java",
                "src/main/java/api/cryptocurrency/MineResource.java",
                "src/main/java/api/cryptocurrency/BlockService.java",
            ],
        }
    ]
