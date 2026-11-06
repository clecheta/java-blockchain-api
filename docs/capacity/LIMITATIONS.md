# Capacity limitations map

Current limitations after the P0-P3 implementation and final local battery on
2026-11-06. Numbers: [CAPACITY_BASELINE.md](CAPACITY_BASELINE.md). Operator
guide: [README.md](README.md).

Evidence: full battery
[`20261106T204026Z`](evidence/20261106T204026Z/) and final secured comparable
throughput
[`20261106T210338Z`](evidence/20261106T210338Z/).

## Executive classification

- **Architecture:** the pending transaction pool is still process memory, and
  acceptance on multiple API replicas does not use a durable, shared
  reservation. The database prevents invalid settlement, but a process restart
  can lose unconfirmed work and two replicas can accept conflicting spends.
- **Source code:** ECDSA signing, JSON handling, HTTP request processing, and
  synchronous PostgreSQL idempotency/persistence remain on the acceptance hot
  path. The local secured ceiling is about 244 accepted transactions/s, well
  below G2's 1,000 transactions/s.
- **Infrastructure:** the evidence is from one laptop, one PostgreSQL instance,
  and one Kafka broker. It proves local correctness and a restore drill, not
  managed HA, regional failover, production PITR, or one-million-account
  operation.

## Architecture

| Current boundary | Evidence | Consequence | Next change |
|------------------|----------|-------------|-------------|
| In-memory pending pool | Restart durability is not part of the battery | Accepted but unsettled transfers can be lost on restart | Durable inbox/outbox or PostgreSQL-backed pending journal |
| Per-process balance reservations | Multi-node settlement is fenced, but acceptance is not globally serialized | Two nodes may accept conflicting spends; settlement rolls one batch back rather than overspending | Atomic database reservation/version check at acceptance |
| Single settlement writer | Root-only + JVM + advisory lock + unique transaction fence passed | Correct, but settlement throughput scales vertically unless partitioning is introduced | Keep one writer until a proven partition key and reconciliation model exist |
| Scheduled batch settlement | 500-item batches settle correctly | Confirmation latency depends on schedule, pool depth, and block duration | Operate to a freshness SLO; tune batch size separately from acceptance |
| Plaintext private keys in PostgreSQL | Current wallet persistence model | Not acceptable custody for real customer funds | KMS/HSM-backed signing and key rotation |

## Source code

Resolved defects include lossless `BigDecimal` round-trip, unsafe-number
rejection, concurrent pool iteration, multiple pending transfers per sender,
incremental balances, bounded settlement receipts, API-key enforcement, and
authenticated peer catch-up.

Remaining hot-path findings:

| Finding | Measured signal | Implication |
|---------|-----------------|-------------|
| Synchronous ECDSA signing and validation | 14,700 accepts in 60.26 s with 64 workers | CPU and allocation cost grows per transfer |
| Request-thread idempotency and database work | acceptance p95 90.8 ms; p99 114.2 ms | Connection-pool and transaction contention limit vertical scale |
| Settlement block construction and persistence | 500-item block p95 4.32 s | Increasing batch size alone can worsen freshness and lock duration |
| Legacy RESTEasy Classic/Mutiny stack | build emits deprecation warning | Migrate to Quarkus REST before framework removal and benchmark again |
| Legacy frontend dependency tree | Node 16 required for build | Operational/security debt, separate from ledger throughput |

## Infrastructure

| Proven locally | Not proven |
|----------------|------------|
| 50,000 wallet create sweep with no failures | Maximum account count or one-million-account latency |
| PostgreSQL restore with 102,322 wallets and 314 blocks | Managed continuous PITR RPO/RTO |
| 243.961 authenticated accepts/s and 202.471 settled units/s | G2 target of 1,000 accepts/s |
| Two-node synchronization after load | Multi-region failover or Kafka HA |
| PostgreSQL advisory-lock single-writer behavior | Cross-region database latency and partition tolerance |

Kafka is fan-out, not the ledger source of truth. MongoDB remains an optional
migration dependency and should not be on the steady-state production path.
The OTLP collector endpoint must be explicitly provisioned in production.

## Gate status

- **G1 passed:** 50,000-wallet sweep and more than 200 authenticated accepted
  transactions/s.
- **G2 remains open:** PostgreSQL ledger and restore mechanics exist, and more
  than 100,000 wallet rows were restored, but 1,000 accepted transactions/s was
  not achieved on this host and a managed PITR exercise is still required.
- **G3 remains open as a capacity gate:** bootstrap API keys, idempotency,
  limits, metrics, and traces exist, but G3 depends on G2 capacity and
  production identity/custody controls.

These are host-specific engineering results, not evidence that the API is a
regulated digital-bank backend. See
[digital-bank positioning](../digital-bank-backend-positioning.md).
