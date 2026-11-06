# Capacity improvement roadmap

This roadmap moves JBlock from the measured local baseline toward a
market-grade private ledger plane that can underpin a digital-bank product.
It does not claim that JBlock alone is a licensed or complete digital bank.

## Baseline and gates

| Gate | Wallets proven | Sustained accepted transactions/s | Purpose |
|------|----------------|-----------------------------------|---------|
| G0 | 2,000 (soft cap) | ~12 | 2026-11-06 baseline |
| G1 | 50,000 | >= 200 | Concurrent pool, balances, funding |
| G2 | 100,000 measured; 500,000 path | >= 1,000 | PostgreSQL ledger and PITR |
| G3 | 1,000,000 design | >= 1,000 with security | Bank control plane |

Accepted transaction throughput and settlement throughput are reported
separately. Mining is batch settlement: production schedules it by interval
and/or pool depth rather than mining every HTTP transfer.

Implementation status on 2026-11-06:

- G1 passed: 50,000-wallet sweep and 243.961 authenticated accepted
  transactions/s on the reference host.
- P2 ledger mechanics passed: PostgreSQL source of truth, advisory locking,
  unique settlement fencing, and a restore drill with 102,322 wallets.
- G2 remains open because the local result is below 1,000 accepted
  transactions/s and managed-environment PITR has not been exercised.
- P3 bootstrap controls are implemented, but G3 remains open because it
  inherits the unmet G2 capacity target and still needs production identity
  and custody controls.

## Multi-cloud settlement model

The application remains cloud-neutral. Production uses the equivalent managed
scheduler in each cloud:

| Cloud | Scheduler |
|-------|-----------|
| AWS | EventBridge Scheduler |
| GCP | Cloud Scheduler |
| Azure | Logic Apps recurrence or a timer-triggered Function |

Each scheduler calls one authenticated settlement endpoint with concurrency
limited to one. Only the settlement/root role may mine; peer replicas accept
transactions and synchronize blocks.

Concurrency is fenced at five levels:

1. A settlement-role configuration flag prevents peers from mining.
2. A JVM mutex makes settlement single-flight and snapshots the pool safely.
3. PostgreSQL advisory locking prevents two processes or duplicate cloud ticks
   from settling the same batch after the PostgreSQL migration.
4. The writer resynchronizes from the authoritative PostgreSQL chain.
5. A unique durable transaction-ID fence prevents duplicate settlement after
   sequential or simultaneous delivery to different nodes.

Quarkus in-process scheduling is for local/development and portable fallback
use. It is disabled by default on peer profiles and must not run on every
production replica.

## P0 — Stabilize money and concurrency

1. Enforce a lossless money contract: decimal amounts are JSON strings.
2. Replace `WalletCodec.longValue()` with full-scale `BigDecimal` persistence.
3. Make mining single-flight and pool snapshots safe under concurrent accepts.
4. Extend the capacity battery with negative precision and concurrency tests.

Exit: no concurrent-modification failure; lossless money round-trip; battery
green.

## P1 — Increase hot-path capacity

1. Support multiple nonce-ordered pending transactions per sender.
2. Materialize balances incrementally instead of scanning the whole chain on
   every settlement.
3. Add an authorized funding/credit endpoint.
4. Add portable scheduled settlement and prove two-node single-writer behavior.

Exit G1: at least 200 accepted transactions/s and 50,000 wallets on the same
host class, with root-only scheduled settlement.

## P2 — Durable PostgreSQL ledger

PostgreSQL becomes the queryable ledger source of truth; Kafka remains the
messaging and fan-out plane.

1. Append-only blocks/transactions and constrained wallet rows.
2. `NUMERIC` money columns and indexes for height, wallet, and transaction IDs.
3. Migration and cutover without losing history.
4. Backup, point-in-time recovery, and an advisory settlement lock.

Exit G2: at least 1,000 accepted transactions/s on a sized environment,
100,000 wallets measured, and one successful PITR drill.

## P3 — Bank control plane

1. Service/client authentication and role-based authorization.
2. Idempotency keys, account velocity limits, and settlement/funding roles.
3. Metrics, traces, and SLOs for accept, settle, persist, and sync.
4. Re-run the capacity battery with security enabled.

Exit G3: G2 capacity remains available with authentication and controls.

## Delivery order

P0 -> P1 -> P2 -> P3. Do not migrate the current slow pool/balance model to
PostgreSQL before P0 and P1 establish correct hot-path semantics.

## Related

- [Capacity testing guide](README.md)
- [Capacity baseline](CAPACITY_BASELINE.md)
- [Limitations map](LIMITATIONS.md)
- [Multi-cloud settlement scheduling](CLOUD_SETTLEMENT_SCHEDULING.md)
- [PostgreSQL ledger runbook](POSTGRES_LEDGER_RUNBOOK.md)
- [Ledger observability and initial SLOs](OPERATIONS_SLOS.md)
- [Digital-bank backend positioning](../digital-bank-backend-positioning.md)
- GitLab [api#31](https://gitlab.com/cleberlecheta/java-blockchain-api/-/issues/31),
  [api#32](https://gitlab.com/cleberlecheta/java-blockchain-api/-/issues/32),
  and [api#43](https://gitlab.com/cleberlecheta/java-blockchain-api/-/issues/43)
