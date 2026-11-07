# Capacity limitations map

Current limitations after durable pending reservations (Flyway V3), boot
hydrate-from-ledger, and the escalated secured battery on 2026-11-06 /
2026-11-07. Numbers: [CAPACITY_BASELINE.md](CAPACITY_BASELINE.md). Operator
guide: [README.md](README.md). Isolation matrix:
[`scripts/capacity/run_isolation_matrix.py`](../../scripts/capacity/run_isolation_matrix.py).

Evidence: escalated battery
[`20261107T003922Z`](evidence/20261107T003922Z/) and prior G1 secured comparable
[`20261106T210338Z`](evidence/20261106T210338Z/).

## Executive classification

- **Architecture:** acceptance journals into PostgreSQL with shared
  `reserved_balance`; settlement is a single root writer. Boot now
  **hydrates** persisted chains without full peer revalidation
  (`ledger.boot.trust-persisted-chain=true`).
- **Source code:** ECDSA signing, JSON, HTTP, and synchronous idempotency /
  reservation dominate accept cost. Best local secured accept rate ~**512 tx/s**.
- **Infrastructure (split):** do not treat “infra” as one bucket — separate
  **host**, **PostgreSQL**, and **Kafka/Confluent** using the isolation matrix.

## How to decide the failure layer

Run `scripts/capacity/run_isolation_matrix.py` on the same build. Compare cells:

| If… | Primary layer |
|-----|----------------|
| Cliff vanishes on a larger host with same PG schema/code | **Host infrastructure** |
| Cliff worsens only as `ledger_wallet` grows; EXPLAIN seq scans / lock waits | **PostgreSQL** |
| Cliff persists on empty/small DB with same workers | **Source code** hot path |
| Correctness breaks (overspend, duplicate settle) while TPS looks fine | **Architecture** |
| Accept TPS rises with `-Dledger.diagnostic.skip-transaction-broadcast=true` | **Kafka coupling** (fan-out, not SoT) |
| Boot/restart misses ready budget after volume | **Architecture + code** (load path) |

### Failure-layer verdict (local 1M matrix)

Evidence: [`isolation-1m-20261107T013017Z`](evidence/isolation-1m-20261107T013017Z/)
(`isolation_summary.json`, `failure_layer_verdict.json`).

| Cliff / cell | Measured | Primary layer | Next change |
|--------------|----------|---------------|-------------|
| Bulk populate 1M wallets | ~69k rows/s; table ~292 MiB | PostgreSQL tooling OK | Stage 10M/40M on sized PG |
| Index health at 1M | ok (no seq-scan fails on keyed lookups) | Not a PG plan cliff at 1M | Re-check at 10M+ |
| Suite D on 1M ledger | 500 accepted = 500 settled; no negatives | Architecture OK | Keep durable reservation |
| Baseline accept TPS | **545.3**/s (100 pairs, 64 workers, 60 s) | **Source code** (primary) | ECDSA/HTTP/ORM hot path; host secondary |
| Kafka-quiet accept TPS | **626.4**/s (~+15%) | Kafka coupling (secondary) | Not the G2 blocker; fan-out only |
| Restore drill at 1M | ok (~15 s) | PostgreSQL OK locally | Managed PITR still open |
| Boot restart + hydrate | ready in tens of ms | Boot confounder mitigated | Keep `trust-persisted-chain` |

**Plan read:** the volume-diagnosis objective at **1M** is met on this laptop.
Answer to “code vs PG vs Kafka?” locally: **mostly source code**, with a small
Kafka publish tax. That does **not** close scale. Tests must next evolve on a
sized host through **5M → 10M → 50M → 100M accounts** while stressing
**high/realistic transfer amounts** (Suite A/C funding and settlement volume),
not only unit-sized TPS filler. Mid-market checkpoint ~40M; stretch bar 100M.
Tracked on [api#20](https://gitlab.com/cleberlecheta/java-blockchain-api/-/issues/20).

## Architecture

| Current boundary | Evidence | Consequence | Next change |
|------------------|----------|-------------|-------------|
| Durable pending + reserved balance | Suites D/E passed after clean drain; peer mine stays `403` | Conflicting multi-node spends no longer overspend | Retention/TTL and operator drain metrics |
| In-memory pool mirror | Pool still mirrors accepted txs for local views/broadcast | Process-local pool can diverge until settle/sync | Treat pool as cache; journal is SoT |
| Single settlement writer | Root-only + JVM + advisory lock + unique transaction fence passed | Correct vertical scale limit | Keep one writer until a proven partition key exists |
| Scheduled batch settlement | Mine-every sweep 100–2000 | Freshness vs accept TPS tradeoff | Operate to settlement SLO |
| Boot hydrate from ledger | `hydrateFromLedger` skips full `isValidTransactionData` replay | Large history no longer CPU-spins on every restart | Keep peer catch-up incremental; forensic rebuilds set `ledger.boot.trust-persisted-chain=false` |
| Plaintext private keys in PostgreSQL | Current wallet persistence model | Not acceptable custody for customer funds | KMS/HSM-backed signing |

## Source code

| Finding | Measured signal | Implication |
|---------|-----------------|-------------|
| Synchronous ECDSA signing and validation | ~512 accepts/s best escalated mine-every | CPU grows per transfer |
| Request-thread idempotency and reservation | accept p95 ~223 ms under 128 workers | Pool/row-lock contention |
| Settlement block construction | mine p95 multi-second under load | Batch size alone can worsen freshness |
| Same-wallet velocity / reservation | Suite D accepted subset without negatives | Controls reject excess correctly |
| Legacy RESTEasy Classic/Mutiny | build deprecation warning | Migrate to Quarkus REST and re-benchmark |

## Infrastructure (split)

### Host

| Proven locally | Not proven |
|----------------|------------|
| 100,000 wallet HTTP create sweep | That this laptop bounds production |
| ~512 authenticated accepts/s | ≥1,000 accepts/s on a sized host |

If the isolation matrix empty-DB cell matches the populated TPS ceiling, the
next upgrade is **CPU/heap for the API process**, not PostgreSQL row count.

### PostgreSQL

| Proven locally | Not proven |
|----------------|------------|
| Durable pending + reserved_balance (V3) | 10M–40M account latency/PITR RPO-RTO |
| Index health gate script for hot lookups | Managed HA / cross-region lag |
| Bulk synthetic populate for fat-table pressure | That bulk rows equal API-created ECDSA wallets |

Kafka is **not** the ledger SoT. Account-scale cliffs that appear only after
`--populate-accounts` point here (or at architecture hot-row locking).

### Kafka / Confluent

| Proven locally | Not proven |
|----------------|------------|
| Fan-out + two-node tip sync after load | Multi-broker HA, multi-region failover |
| Diagnostic skip of transaction broadcast | Confluent Cloud partition/throughput sizing |

Use `--kafka-quiet` on the isolation matrix **after** restarting the API with
`-Dledger.diagnostic.skip-transaction-broadcast=true`. If accept TPS jumps,
the cliff includes **Kafka publish coupling**, not reservation SoT.

## Gate status

- **G1 passed:** 100,000-wallet hard-cap sweep and >200 authenticated accepts/s.
- **G2 remains open:** 1,000 accepts/s and managed PITR not proven on this host.
- **Account ladder still open:** evolve evidence **5M → 10M → 50M → 100M**
  with high/real value amounts
  ([digital-bank positioning](../digital-bank-backend-positioning.md));
  local proof remains 100k HTTP + 1M isolation matrix.

These are host-specific engineering results, not evidence that the API is a
regulated digital-bank backend.
