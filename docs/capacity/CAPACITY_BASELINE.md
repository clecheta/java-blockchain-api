# Capacity baseline

Host-specific Phase-1 results after P0-P3 implementation on **2026-11-06**.
How to reproduce and interpret the battery: [README.md](README.md). Limit
classification: [LIMITATIONS.md](LIMITATIONS.md).

| Field | Value |
|-------|-------|
| Full canonical battery | [`evidence/20261106T204026Z/`](evidence/20261106T204026Z/) |
| Final secured comparable throughput | [`evidence/20261106T210338Z/`](evidence/20261106T210338Z/) |
| Machine | 13th Gen Intel i7-1355U, 12 threads, ~30 GiB RAM |
| Runtime | Two Quarkus JVMs, PostgreSQL 17, Kafka, authentication and idempotency enabled |

## Money

| Metric | Last-good | First-fail |
|--------|-----------|------------|
| Single settled transfer | `9223372036854775807` (`Long.MAX_VALUE`) | none in probe ladder |
| Multi-chunk settled volume | `5000` | none |

Amounts use the lossless JSON-string contract and `BigDecimal` storage. JSON
numbers are accepted only when they are safe integers; floating-point and
unsafe integer numbers are rejected.

## Wallets

| Metric | Result |
|--------|--------|
| One-run create sweep | **50,000** wallets, zero failures |
| Create latency | p50 56.8 ms; p95 123.3 ms; p99 165.9 ms |
| Stop reason | configured hard cap, not a discovered failure |
| PostgreSQL backup/restore population | **102,322** wallets and 314 blocks |

The sweep proves 50,000 successful creates. The restore drill proves storage
and recovery above 100,000 rows; it is not a one-run 100,000-create latency
claim.

## Transfer acceptance and settlement

Final secured, instrumented, comparable trial (100 funded senders, 64 HTTP
workers, 60-second acceptance window):

| Metric | Result |
|--------|--------|
| Accepted | **14,700**, zero failures |
| Accepted throughput | **243.961 transactions/s** |
| Acceptance latency | p50 49.8 ms; p95 90.8 ms; p99 114.2 ms |
| Settled | **14,700** in 30 bounded blocks |
| End-to-end settlement throughput | **202.471 transactions/s** |
| Settlement latency per 500-item block | p50 1.78 s; p95 4.32 s; max 5.05 s |

The full canonical run before the final security/telemetry pass accepted
15,400 transfers at **255.586 transactions/s** and settled all of them at
**190.811 transactions/s**. Both runs clear G1's 200 accepted
transactions/s target. Neither approaches G2's 1,000 transactions/s target.

## Correctness and operations checks

- Root-only settlement authorization: passed.
- Two-node Kafka synchronization and catch-up: passed after every final trial.
- Concurrent peer settlement protection: role gate plus JVM and PostgreSQL
  advisory locks, with durable unique transaction-ID fencing.
- PostgreSQL backup/restore drill: passed.
- API security probes: unauthenticated client request `401`; client key on
  settlement `401`; service key on empty settlement `204`.
