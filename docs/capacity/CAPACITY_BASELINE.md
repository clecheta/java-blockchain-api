# Capacity baseline

Host-specific Phase-1 results after durable pending reservations and the
escalated secured battery on **2026-11-06 / 2026-11-07**. How to reproduce and
interpret the battery: [README.md](README.md). Limit classification:
[LIMITATIONS.md](LIMITATIONS.md).

| Field | Value |
|-------|-------|
| Prior G1 secured comparable | [`evidence/20261106T210338Z/`](evidence/20261106T210338Z/) |
| Escalated secured battery (canonical) | [`evidence/20261107T003922Z/`](evidence/20261107T003922Z/) |
| Isolation matrix at 5M accounts | [`evidence/isolation-5m-20261107T154100Z/`](evidence/isolation-5m-20261107T154100Z/) |
| Machine | 13th Gen Intel i7-1355U, 12 threads, ~30 GiB RAM |
| Runtime | Two Quarkus JVMs, PostgreSQL 17, Kafka (Postgres-only ledger; no Mongo), authentication and idempotency enabled, Flyway V3 durable pending |

## Money

| Metric | Last-good | First-fail |
|--------|-----------|------------|
| Single settled transfer | `9223372036854775807` (`Long.MAX_VALUE`) | none in probe ladder |
| Multi-chunk settled volume | `1,000,000` | none |

Amounts use the lossless JSON-string contract and `BigDecimal` storage. JSON
numbers are accepted only when they are safe integers; floating-point and
unsafe integer numbers are rejected.

## Wallets

| Metric | Result |
|--------|--------|
| One-run create sweep | **100,000** wallets, zero failures |
| Create latency | p50 86.3 ms; p95 156.5 ms; p99 193.0 ms |
| Stop reason | configured hard cap, not a discovered failure |
| Bulk synthetic populate (isolation 5M) | **5,000,000** `ledger_wallet` rows at ~60.5k rows/s |

## Required account-volume stages

Mandatory local runs. Account rows without settled movement do not pass.
Completion rule and drivers: [README.md](README.md), [SPEC.md](SPEC.md).

| Stage | Accounts present | Cumulative units registered and moved | Status on 2026-11-07 |
|-------|------------------|---------------------------------------|----------------------|
| V5 | 5,000,000 | 150,000,000,000 | **Passed** — [`volume-v5-20261107T182239Z`](evidence/volume-v5-20261107T182239Z/) |
| V10 | 10,000,000 | 300,000,000,000 | Not run |
| V50 | 50,000,000 | 600,000,000,000 | Not run |
| V100 | 100,000,000 | 1,100,000,000,000 | Not run |

### V5 movement (passed)

| Field | Value |
|-------|-------|
| Evidence (canonical) | [`volume-v5-20261107T182239Z`](evidence/volume-v5-20261107T182239Z/) |
| Prior failed attempt | [`volume-v5-20261107T180717Z`](evidence/volume-v5-20261107T180717Z/) (~6.1B then JDBC acquisition timeout) |
| Accounts present | 5,001,010 |
| Accepted / settled | 150,000 transfers / **150,000,000,000** units |
| Elapsed | ~418 s |
| Negatives | 0 |
| Destination balance sum | 150,000,000,000 |
| Accept latency | p50 75.1 ms; p95 175.1 ms; p99 236.0 ms |
| Mine latency | p50 774 ms; p95 3.27 s (300 successful mines) |
| Workers / pool | 64 workers; Agroal `max-size=96`; 0 acquisition timeouts |

## Isolation matrix at 5M (Postgres-only)

| Cell | Result |
|------|--------|
| Baseline accept TPS | **602.928** tx/s (100 pairs, 60 s; 36,199 ok / 1 fail) |
| Suite D contention | **ok** — 500 accepted = 500 settled; no negatives |
| Restore drill | **ok** (~24.8 s) |
| Boot restart | **ok** — root ready ~9 ms |
| Index health | wallet keyed Index Scan; `settled_by_tx` Seq Scan on tiny fence table (PK present) |

## Transfer acceptance and settlement

Escalated secured mine-every sweep (200 funded senders, 128 HTTP workers,
180-second acceptance window, auth + idempotency on):

| `mine_every` | Accepted throughput | Accept p50 / p95 | Settled $/s (suite metric) |
|--------------|---------------------|------------------|----------------------------|
| 100 | 227.813 tx/s | — | 168.473 |
| 500 | 282.131 tx/s | p50 111.5 ms; p95 223.8 ms | 189.313 |
| 1000 | 230.200 tx/s | — | 128.805 |
| **2000** | **511.985 tx/s** | p50 118.2 ms; p95 222.9 ms | 169.804 |

Best local accepted throughput in this escalated run is **511.985 tx/s** at
`mine_every=2000` (92,173 accepts in the 180 s window, 0.46% error rate). That
clears G1 and improves on the prior secured comparable of **243.961 tx/s**, but
still does not meet G2's **1,000** accepted transactions/s on this laptop.

Prior secured comparable for continuity (100 senders, 64 workers, 60 s,
`mine_every≈500`): **243.961** accepted tx/s and **202.471** settled units/s
in [`20261106T210338Z`](evidence/20261106T210338Z/).

## Correctness suites (durable shared reservation)

Re-measured after draining leftover pending from throughput sweeps:

| Suite | Result |
|-------|--------|
| D — same-wallet contention | **ok** — accepted 878, settled 878, final balance matched expected, no negative balance |
| E — two-node concurrent accept | **ok** — accepted 950 across root+peer, settled 950, peer mine `403`, tip hashes matched |
| Smoke two-node | **ok** |

Not every concurrent attempt is accepted under velocity and reservation
pressure; rejected spends stay non-negative and settled counts match accepted
counts for the suite sender after a clean drain.

## Correctness and operations checks

- Root-only settlement authorization: passed.
- Two-node Kafka synchronization and catch-up: passed after the escalated run.
- Concurrent peer settlement protection: role gate plus JVM and PostgreSQL
  advisory locks, with durable unique transaction-ID fencing.
- Durable pending journal + `reserved_balance`: Flyway V3; accepts serialize
  spend across nodes; unsettled work survives process boundaries in PostgreSQL.
- API security probes: unauthenticated client request `401`; client key on
  settlement `401`; service key on empty settlement `204`.
