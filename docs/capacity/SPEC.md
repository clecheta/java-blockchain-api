# Capacity battery — specification

## Environment

| Item | Value |
|------|-------|
| App | Single root Quarkus JVM on `:8018` |
| Infra | Docker Compose PostgreSQL 17 + Kafka 3.8 |
| Units | Ledger `$` (integer-like amounts), not FX |
| Funding | HTTP create + `POST /W4LL37/fUND` (create always starts at 0) |
| Post-check | `scripts/smoke-two-nodes.sh` at comfortable load (not part of ceiling search) |

## Account-volume stages (required)

These stages run to completion on the local machine. Ceiling suites A–C do not
replace them. Pass/fail for a stage ignores wall-clock time and host saturation.

| Stage | Accounts present | Cumulative ledger units registered and moved |
|-------|------------------|-----------------------------------------------|
| V5 | 5,000,000 | 150,000,000,000 |
| V10 | 10,000,000 | 300,000,000,000 |
| V50 | 50,000,000 | 600,000,000,000 |
| V100 | 100,000,000 | 1,100,000,000,000 |

**Registered and moved:** sum of amounts funded, transferred, and settled while
that account population is present. Bulk `ledger_wallet` inserts and unmoved
balances do not count.

**Pass:** both targets reached; no negative balances; settled movement equals
accepted movement for the exercised funds; destination balances match.

**Fail:** process abort, correctness break, or a stop before either target.
A time budget, a p95 comfort stop, or 90–100% CPU, memory, or disk use is not
a fail and is not a pass. Do not shrink the target.

**Driver for V5:** `scripts/capacity/run_volume_stage.py`. Defaults settle
150,000 transfers of 1,000,000 while 5,000,000 wallet rows are present.
Synthetic bulk rows provide the account population; ECDSA wallets run
fund → transact → mine. Stop reasons are only `target_reached` or
`correctness_failed`. Duration and host saturation are ignored.

Later stages may add local API nodes (PostgreSQL stays the source of truth;
root stays the only settlement writer) or JMeter / k6 / Gatling as the HTTP
driver with the same balance check.

Evidence so far: 5,000,000 account rows in
`evidence/isolation-5m-20261107T154100Z/`; V5 movement **passed** in
`evidence/volume-v5-20261107T182239Z/` (150 billion settled, ~418 s). Later
stages (V10 / V50 / V100) remain open for operator-owned full runs.

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
