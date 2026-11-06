"""Timing, percentiles, and light host samples for capacity evidence."""

from __future__ import annotations

import json
import os
import platform
import time
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any


@dataclass
class LatencyTracker:
    samples_ms: list[float] = field(default_factory=list)

    def add(self, elapsed_s: float) -> None:
        self.samples_ms.append(elapsed_s * 1000.0)

    def percentile(self, p: float) -> float | None:
        if not self.samples_ms:
            return None
        ordered = sorted(self.samples_ms)
        if len(ordered) == 1:
            return ordered[0]
        rank = (p / 100.0) * (len(ordered) - 1)
        lo = int(rank)
        hi = min(lo + 1, len(ordered) - 1)
        frac = rank - lo
        return ordered[lo] * (1 - frac) + ordered[hi] * frac

    def summary(self) -> dict[str, Any]:
        return {
            "count": len(self.samples_ms),
            "p50_ms": self.percentile(50),
            "p95_ms": self.percentile(95),
            "p99_ms": self.percentile(99),
            "max_ms": max(self.samples_ms) if self.samples_ms else None,
            "min_ms": min(self.samples_ms) if self.samples_ms else None,
        }


def machine_fingerprint() -> dict[str, Any]:
    mem_total_kb = None
    mem_available_kb = None
    try:
        with open("/proc/meminfo", encoding="utf-8") as fh:
            for line in fh:
                if line.startswith("MemTotal:"):
                    mem_total_kb = int(line.split()[1])
                elif line.startswith("MemAvailable:"):
                    mem_available_kb = int(line.split()[1])
    except OSError:
        pass

    cpu_model = platform.processor() or None
    try:
        with open("/proc/cpuinfo", encoding="utf-8") as fh:
            for line in fh:
                if line.startswith("model name"):
                    cpu_model = line.split(":", 1)[1].strip()
                    break
    except OSError:
        pass

    return {
        "hostname": platform.node(),
        "platform": platform.platform(),
        "python": platform.python_version(),
        "cpu_count": os.cpu_count(),
        "cpu_model": cpu_model,
        "mem_total_kib": mem_total_kb,
        "mem_available_kib": mem_available_kb,
        "captured_at": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
    }


def sample_host() -> dict[str, Any]:
    out: dict[str, Any] = {"ts": time.time()}
    try:
        with open("/proc/loadavg", encoding="utf-8") as fh:
            parts = fh.read().split()
            out["load1"] = float(parts[0])
            out["load5"] = float(parts[1])
            out["load15"] = float(parts[2])
    except OSError:
        pass
    try:
        with open("/proc/meminfo", encoding="utf-8") as fh:
            for line in fh:
                if line.startswith("MemAvailable:"):
                    out["mem_available_kib"] = int(line.split()[1])
                    break
    except OSError:
        pass
    return out


def write_json(path: Path, payload: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, indent=2, default=str) + "\n", encoding="utf-8")


class Stopwatch:
    def __init__(self) -> None:
        self.t0 = time.perf_counter()

    def elapsed(self) -> float:
        return time.perf_counter() - self.t0
