# Capacity limitations map

Current limitations after durable pending reservations (Flyway V3) and the
escalated secured battery on 2026-11-06 / 2026-11-07. Numbers:
[CAPACITY_BASELINE.md](CAPACITY_BASELINE.md). Operator guide: [README.md](README.md).

Evidence: escalated battery
[`20261107T003922Z`](evidence/20261107T003922Z/) and prior G1 secured comparable
[`20261106T210338Z`](evidence/20261106T210338Z/).

## Executive classification

- **Architecture:** acceptance now journals into PostgreSQL with a shared
  `reserved_balance`, so cross-node conflicting spends are rejected instead of
  overspending. Settlement remains a single root writer. ECDSA signing and
  per-request DB work still dominate the accept path; G2 is still open on this
  laptop.
- **Source code:** ECDSA signing, JSON handling, HTTP request processing, and
  synchronous PostgreSQL idempotency/reservation remain on the acceptance hot
  path. Best escalated secured accept rate here is about **512 tx/s**, still
  below G2's 1,000 tx/s.
- **Infrastructure:** the evidence is from one laptop, one PostgreSQL instance,
  and one Kafka broker. It proves local correctness and a restore drill path,
  not managed HA, regional failover, production PITR, or million-account
  latency.

## Architecture

| Current boundary | Evidence | Consequence | Next change |
|------------------|----------|-------------|-------------|
| Durable pending + reserved balance | Suites D/E passed after clean drain; peer mine stays `403` | Conflicting multi-node spends no longer overspend; unsettled work is in PostgreSQL | Keep journal; add retention/TTL and operator drain metrics |
| In-memory pool mirror | Pool still mirrors accepted txs for local views/broadcast | Process-local pool can diverge until settle/sync; not the SoT | Treat pool as cache; prefer journal for recovery |
| Single settlement writer | Root-only + JVM + advisory lock + unique transaction fence passed | Correct, but settlement throughput scales vertically unless partitioning is introduced | Keep one writer until a proven partition key and reconciliation model exist |
| Scheduled batch settlement | 100–2000 mine-every sweep; larger batches raise accept TPS but lag settle freshness | Confirmation latency depends on schedule, pool depth, and block duration | Operate to a freshness SLO; tune batch size separately from acceptance |
| Plaintext private keys in PostgreSQL | Current wallet persistence model | Not acceptable custody for real customer funds | KMS/HSM-backed signing and key rotation |

## Source code

Resolved defects include lossless `BigDecimal` round-trip, unsafe-number
rejection, concurrent pool iteration, multiple pending transfers per sender,
incremental balances, bounded settlement receipts, API-key enforcement,
authenticated peer catch-up, and PostgreSQL-backed pending reservations.

Remaining hot-path findings:

| Finding | Measured signal | Implication |
|---------|-----------------|-------------|
| Synchronous ECDSA signing and validation | 92,173 accepts in 180 s at best mine-every | CPU and allocation cost grows per transfer |
| Request-thread idempotency and reservation | accept p95 ~223 ms under 128 workers | Connection-pool and row-lock contention limit vertical scale |
| Settlement block construction and persistence | mine p95 multi-second under load | Increasing batch size alone can worsen freshness and lock duration |
| Same-wallet velocity / reservation pressure | Suite D accepted 878/1000 without negatives | Controls correctly reject excess; tune velocity for product SLOs |
| Legacy RESTEasy Classic/Mutiny stack | build emits deprecation warning | Migrate to Quarkus REST before framework removal and benchmark again |

## Infrastructure

| Proven locally | Not proven |
|----------------|------------|
| 100,000 wallet create sweep with no failures | Maximum account count or one-million-account latency |
| Prior restore drill with 102,322 wallets and 314 blocks | Managed continuous PITR RPO/RTO |
| 511.985 authenticated accepts/s (best escalated mine-every) | G2 target of 1,000 accepts/s |
| Two-node synchronization after load | Multi-region failover or Kafka HA |
| PostgreSQL advisory-lock single-writer behavior | Cross-region database latency and partition tolerance |

Kafka is fan-out, not the ledger source of truth. MongoDB remains an optional
migration dependency and should not be on the steady-state production path.
The OTLP collector endpoint must be explicitly provisioned in production when
trace export is required.

## Gate status

- **G1 passed:** 50,000+ wallet sweeps and more than 200 authenticated accepted
  transactions/s (now also a 100,000-wallet hard-cap sweep).
- **G2 remains open:** PostgreSQL ledger, durable pending, and restore mechanics
  exist, and 100,000 wallets were created in one run, but 1,000 accepted
  transactions/s was not achieved on this host and a managed PITR exercise is
  still required.
- **G3 remains open as a capacity gate:** bootstrap API keys, idempotency,
  limits, metrics, and traces exist, but G3 depends on G2 capacity and
  production identity/custody controls.

These are host-specific engineering results, not evidence that the API is a
regulated digital-bank backend. See
[digital-bank positioning](../digital-bank-backend-positioning.md).
