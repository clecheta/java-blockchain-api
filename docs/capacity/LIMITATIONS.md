# Capacity limitations map

Where limits sit after Phase-1 battery (2026-11-06). Numbers: [CAPACITY_BASELINE.md](CAPACITY_BASELINE.md). Guide: [README.md](README.md).

Canonical evidence: [`20261106T185800Z`](evidence/20261106T185800Z/) (full A+B+C+smoke).  
Supporting: [`20261106T185200Z`](evidence/20261106T185200Z/) (A), [`20261106T183500Z`](evidence/20261106T183500Z/) (B/C), [`20261106T183209Z`](evidence/20261106T183209Z/) (CME / harness lessons).

## Executive map

- **Architecture:** Settlement is gated by **explicit mine** and **one pending tx per sender**; HTTP create always funds **0** (Mongo seed required). Sustained throughput on this host ≈ **12.1 tx/s** with 16 pairs — design-bound more than CPU-bound.
- **Source code:** `WalletCodec` persists balances through **`longValue()`** (~`Long.MAX_VALUE` ceiling). Pool/mine path can throw **`ConcurrentModificationException`** under concurrent transact+mine. JSON **number** amounts above ~`2^53−1` corrupt values unless clients send amount as a **string**.
- **Infrastructure:** Mongo + Kafka + this laptop handled Suite A through `Long.MAX_VALUE`, **2000** wallet creates, and Suite C without ready-probe failure; smoke multi-node sync still OK. Soft wallet stop was **test budget**, not an infra cliff.

## Architecture limitations

| Finding | Suite | Last-good | First-fail | Evidence | Implication for ledger-as-bank |
|---------|-------|-----------|------------|----------|--------------------------------|
| Explicit mine required to settle; one pending tx per sender | design / C | ~12.06 tx/s @ 16 pairs | — | `PoolResource`, `MineResource`, `suite_throughput.json` | Not an always-on payments rail; settlement policy is operator-driven |
| HTTP create balance always 0; funding needs Mongo seed | design | — | — | `WalletResource`, `CapacityWalletSeeder` | No product “deposit” API; custody/funding model incomplete |
| Full-chain balance recompute on touch | design | — | — | `BlockService` | Latency will grow with chain length (scalability pillar) |
| Throughput parallelism ≈ distinct funded senders | C | 16 pairs → ~12.06 tx/s | — | `20261106T185800Z/suite_throughput.json` | Hot wallet cannot scale TPS; need many accounts or redesign pool |

## Source-code limitations

| Finding | Suite | Last-good | First-fail | Code path | Evidence |
|---------|-------|-----------|------------|-----------|----------|
| Balance codec uses `BigInteger.longValue()` on decode | A (latent) | `Long.MAX_VALUE` settled | values &gt; signed long (not probed as first-fail) | `WalletCodec.java` | Hard integer ceiling for persisted balances |
| `ConcurrentModificationException` on mine under concurrent pool updates | C (diag) | partial | mine 500 | transaction pool iteration vs Kafka/HTTP writers | `20261106T183209Z/suite_throughput.json` |
| Amount as JSON **number** loses precision &gt; ~`2^53−1` | A (API contract / clients) | safe ints below 2^53 | corrupted amounts near/above 2^53 | `TransactionCommand.amount` + JSON binding | Clients **must** send amount as string (harness now does); document in API contract |
| Historical Kafka/~10k anecdote | A | `Long.MAX_VALUE` OK | not reproduced | — | Do not treat old `MoneyFlowTest` comment as current ceiling without re-proof |

## Infrastructure limitations

| Finding | Suite | Last-good | First-fail | Component | Evidence |
|---------|-------|-----------|------------|-----------|----------|
| No ready/OOM/disk cliff at 2000 creates | B | 2000 | soft `hard_cap` | Mongo + JVM + host | `20261106T185800Z/suite_wallets.json` — raise cap to hunt further |
| Host completed Suite C without probe failure | C | duration complete | — | i7-1355U | `suite_throughput.json` host_samples |
| Multi-node Kafka sync OK after load | smoke | tip match | — | Kafka + peer catch-up | `smoke_two_nodes.json` |
| Compose single-broker / single-Mongo | all | — | — | local Docker | Not a HA or PITR story — feeds [api#43](https://gitlab.com/cleberlecheta/java-blockchain-api/-/issues/43) |

## What this does not mean

- Ceilings are **host-specific** (see `machine.json`).
- This API is still **not** a regulated digital-bank backend ([positioning](../digital-bank-backend-positioning.md)).
- Results **inform** [api#43](https://gitlab.com/cleberlecheta/java-blockchain-api/-/issues/43); they do not alone decide Postgres vs Mongo.
- Soft wallet cap ≠ proven maximum account capacity.

## Next levers by layer

| Layer | Recommended next action (not done in this delivery) |
|-------|-----------------------------------------------------|
| Architecture | Define settlement policy (who may mine, auto-mine rules) and pool concurrency model; add an explicit funding/deposit path if this stays a ledger plane |
| Source code | Replace `WalletCodec` long decode with full `BigInteger`/`BigDecimal` round-trip; synchronize or copy-on-iterate the transaction pool during mine; document amount-as-string in the public API |
| Infrastructure | Re-run Suite B with higher hard cap; size Kafka/Mongo for target TPS; use evidence in api#43 datastore spike (PITR, indexes, query model) |
