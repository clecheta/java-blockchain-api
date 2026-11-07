# Capacity battery — specification

## Environment

| Item | Value |
|------|-------|
| App | Single root Quarkus JVM on `:8018` |
| Infra | Docker Compose PostgreSQL 17 + Kafka 3.8 |
| Units | Ledger `$` (integer-like amounts), not FX |
| Funding | HTTP create + `POST /W4LL37/fUND` (create always starts at 0) |
| Post-check | `scripts/smoke-two-nodes.sh` at comfortable load (not part of ceiling search) |

## Suites

### A — Money ceiling

1. Geometric probe of single-transfer amounts (see `suite_money.PROBE_AMOUNTS`).
2. On first fail after a success, binary-search last-good / first-fail.
3. Settled-volume ramp: many chunks of `min(100, last_good)` until fail or 50 chunks.

**Pass (trial):** HTTP transact 2xx, mine 2xx, destination balance equals amount (within integer decode).

**Fail:** any non-2xx, exception, readiness loss, or balance mismatch.

### B — Wallet ceiling

Parallel `POST /W4LL37/cRE47E` in geometric batches. Stop on create failure, ready failure, p95 latency &gt; 5000 ms, time budget, or hard cap.

### C — Throughput

`N` funded senders + `N` HTTP destinations; concurrent transfers of 1 unit; mine every 10 pending; run for configured duration. Stop early if error rate &gt; 15% or ready fails.

Report: `tx_per_s`, `mine_per_s`, `dollars_per_s`, latency percentiles, host load samples.

## Limitation layers

Every cliff is tagged with exactly one **primary** layer:

| Layer | Meaning |
|-------|---------|
| `architecture` | Product/design constraints (mine gate, one tx/sender, full-chain balance walk, create@0) |
| `source_code` | Fixable in app/library (serializers, balance bugs, hot path) |
| `infrastructure` | Kafka, PostgreSQL, JVM heap, host CPU/RAM/disk, Docker defaults |

Rules: see `scripts/capacity/lib_classify.py`. Machine-readable: `evidence/<run-id>/limitations.json`.

## Evidence layout

```
docs/capacity/evidence/<run-id>/
  machine.json
  suite_money.json
  suite_wallets.json
  suite_throughput.json
  limitations.json
  summary.json
  smoke_two_nodes.json
```
