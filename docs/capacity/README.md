# JBlock API capacity testing guide

**Start here.** This page explains why we run capacity tests, how to run them, and how to read the results. Technical detail lives in the linked docs; the harness lives under [`scripts/capacity/`](../../scripts/capacity/).

Related product framing: [Digital bank backend positioning](../digital-bank-backend-positioning.md) — these tests bound a **private ledger / settlement plane**, they do **not** certify a regulated digital bank.

---

## 1. Why these tests exist

Ambition to use this API as a fintech **ledger backend** needs honest answers to three questions on a real machine:

| Question | Why it matters |
|----------|----------------|
| How much ledger `$` can we settle? | Wrong balances, Kafka payload cliffs, or codec truncation are reliability risks before any “bank” narrative. |
| How many wallets/accounts can we hold? | Account scale is the first product pressure on the PostgreSQL ledger + API create path. |
| How much can *this host* process? | Throughput (tx/s, mine cycles/s, `$`/s) separates design limits from “the laptop is saturated.” |

Without measured ceilings we guess. Historical stress intent lived on [api#20](https://gitlab.com/cleberlecheta/java-blockchain-api/-/issues/20); datastore scale decisions live on [api#43](https://gitlab.com/cleberlecheta/java-blockchain-api/-/issues/43). Capacity evidence feeds those discussions.

### Phase 1 vs Phase 2

| Phase | Tool | Answers |
|-------|------|---------|
| **1 (this guide)** | Python battery in `scripts/capacity/` | *Where does it break?* last-good / first-fail + failure layer |
| **2 (later)** | JMeter / k6 / Gatling | *How does it behave while healthy?* SLA latency at a known-safe load |

Do **not** replace Phase 1 with “only JMeter.” Settlement here is stateful (seed → transact → mine → verify balance); HTTP RPS alone misses ledger cliffs.

### What we deliberately do *not* claim

- Digital-bank or core-banking readiness.
- Cloud multi-region SLAs.
- That host numbers generalize to production without re-running on a dedicated perf environment.

---

## 2. How to run

### Prerequisites

1. Sibling library installed: `mvn -f ../java-blockchain/pom.xml install -DskipTests`
2. Infra up: `docker compose up -d` (PostgreSQL + Kafka; Mongo remains only
   for optional migration)
3. Root API: `./mvnw compile quarkus:dev` → `http://localhost:8018` ready (`/q/health/ready`)
4. Python 3.10+ deps:

```bash
cd java-blockchain-api
python3 -m venv .venv-capacity
source .venv-capacity/bin/activate
pip install -r scripts/capacity/requirements.txt
```

HTTP wallet create always starts at **balance 0**. The battery creates wallets
through HTTP and credits treasuries through the authenticated
`POST /W4LL37/fUND` endpoint. Override `BANK_CLIENT_API_KEY` and
`BANK_SERVICE_API_KEY` when local defaults are not active.

### Full battery

```bash
python3 scripts/capacity/run_battery.py
```

Evidence lands in `docs/capacity/evidence/<run-id>/` (UTC timestamp by default).

### Faster dry-run

```bash
python3 scripts/capacity/run_battery.py \
  --wallet-hard-cap 200 --wallet-time-budget-s 120 \
  --throughput-duration-s 45 --throughput-pairs 10 \
  --skip-smoke
```

### Useful flags

| Flag | Default | Meaning |
|------|---------|---------|
| `--base-url` | `http://localhost:8018` | Root API |
| `--mongo-uri` | `mongodb://localhost:27017` | Deprecated compatibility flag; funding is HTTP/PostgreSQL |
| `--run-id` | UTC timestamp | Evidence folder name |
| `--wallet-hard-cap` | `5000` | Suite B create ceiling |
| `--wallet-time-budget-s` | `600` | Suite B soft stop |
| `--wallet-workers` | `32` | Concurrent wallet-create workers |
| `--throughput-duration-s` | `90` | Suite C window |
| `--throughput-pairs` | `20` | Concurrent funded senders |
| `--throughput-workers` | `32` | Concurrent HTTP workers |
| `--skip-smoke` | off | Skip multi-node post-check |
| `--skip-money` / `--skip-wallets` / `--skip-throughput` | off | Skip a suite |

Operator notes and suite internals: [`scripts/capacity/README.md`](../../scripts/capacity/README.md). Formal pass/fail rules: [SPEC.md](SPEC.md). Short “why” archive: [INTENT.md](INTENT.md).

### What each suite does

| Suite | Question | Method (short) |
|-------|----------|----------------|
| **A — money** | Max `$` | Fund treasury → `POST /p00L/tRaN5ACt` (**amount as JSON string**) → settle → check balances; geometric probe + binary search; then multi-chunk settled volume |
| **B — wallets** | Max accounts | Parallel `POST /W4LL37/cRE47E` batches until fail, ready drop, p95 budget, time, or hard cap |
| **C — throughput** | Max this host | N funded senders, concurrent transfers, mine every K txs; report tx/s, mine/s, `$`/s |
| **Smoke (post)** | Multi-node still OK? | Existing `scripts/smoke-two-nodes.sh` at a *comfortable* load (not a ceiling hunt) |

Ceiling loops use **root `:8018` only**. Peer sync is the post-check so it does not confound the cliff.

---

## 3. How to evaluate the result

Read artifacts in this order:

```text
1. evidence/<run-id>/summary.json          → quick numbers
2. CAPACITY_BASELINE.md                    → human table of ceilings (update after each serious run)
3. evidence/<run-id>/limitations.json      → machine-tagged cliffs
4. LIMITATIONS.md                          → where limits sit (architecture / source / infrastructure)
5. suite_*.json                            → deep dive / debugging
```

### Step A — Numbers (ceilings)

From `summary.json` / suite files, record:

| Suite | Look for |
|-------|----------|
| A | `last_good_single_amount`, `first_fail_single_amount`, settled volume |
| B | `last_good_wallet_count`, `stop_reason` (`create_failed` vs `time_budget` / `hard_cap`) |
| C | `tx_per_s`, `dollars_per_s`, `mine_per_s`, `error_rate`, `stop_reason` |

Copy them into [CAPACITY_BASELINE.md](CAPACITY_BASELINE.md) with the run id and a pointer to `machine.json` (CPU/RAM fingerprint). Soft stops (time/hard cap without HTTP failure) mean “no cliff found inside the budget,” not “infinite capacity.”

### Step B — Layer (where the limit lives)

Every first-fail should have a **primary** layer. Use [LIMITATIONS.md](LIMITATIONS.md) as the narrative; `limitations.json` as the source tags.

| Layer | Means | Typical signals |
|-------|--------|-----------------|
| **Architecture** | Design/model; changing it redesigns the product | Mine required to settle; one pending tx per sender; create@0; full-chain balance walk; throughput flat while CPU free |
| **Source code** | Fixable in app/library without changing product shape | Balance mismatch near `Long.MAX_VALUE` / `WalletCodec.longValue()`; wrong balance after mine; bad serializer |
| **Infrastructure** | Kafka, Mongo, JVM, host, Compose defaults | Broker/serdes errors; ready probe down; OOM; disk; load saturation; timeouts |

Secondary layers are allowed (e.g. Kafka reject = infrastructure primary, serializer bug = source secondary).

### Step C — Decision checklist

Ask, in order:

1. **Reliability first?** If Suite A shows wrong balances → treat as **blocker** for ledger-as-bank ambition (usually source or codec path).
2. **Is the cliff architectural?** If settlement cannot move without redesign (manual mine, one-tx-per-sender) → product decision, not a “tune Kafka” ticket.
3. **Is the cliff infrastructural?** If Mongo/Kafka/host die first → ops/sizing or datastore work ([api#43](https://gitlab.com/cleberlecheta/java-blockchain-api/-/issues/43)), not a fake API feature.
4. **Did smoke fail after the battery?** Multi-node sync cost may add an infra/architecture constraint even if single-node ceilings look fine.

### Step D — Update the human reports

After a serious run:

1. Fill [CAPACITY_BASELINE.md](CAPACITY_BASELINE.md) with last-good / first-fail tables.
2. Rewrite [LIMITATIONS.md](LIMITATIONS.md) executive map + three layer tables from `limitations.json`.
3. Set one **next lever** per layer (fix code / redesign settlement / retune or replace infra) — do not imply the lever was already implemented.

### Anti-patterns when reading results

- Treating a dry-run (`--wallet-hard-cap 200`) as a production baseline.
- Comparing runs without matching `machine.json`.
- Using Suite C RPS alone to “prove” bank scale.
- Blaming Mongo for an architecture gate (e.g. forgetting to mine).

---

## Document map

| Doc | Role |
|-----|------|
| **[README.md](README.md) (this file)** | Why / how to run / how to evaluate |
| [IMPROVEMENT_ROADMAP.md](IMPROVEMENT_ROADMAP.md) | G0-G3 capacity and bank-ledger delivery roadmap |
| [CLOUD_SETTLEMENT_SCHEDULING.md](CLOUD_SETTLEMENT_SCHEDULING.md) | AWS/GCP/Azure scheduling and multi-node fencing |
| [POSTGRES_LEDGER_RUNBOOK.md](POSTGRES_LEDGER_RUNBOOK.md) | Ledger migration, backup, restore, and fencing |
| [OPERATIONS_SLOS.md](OPERATIONS_SLOS.md) | Metrics, traces, initial SLOs, and required alerts |
| [INTENT.md](INTENT.md) | Short intent + non-goals |
| [SPEC.md](SPEC.md) | Pass/fail and evidence layout |
| [CAPACITY_BASELINE.md](CAPACITY_BASELINE.md) | Latest numeric ceilings |
| [LIMITATIONS.md](LIMITATIONS.md) | Latest layer map |
| `evidence/<run-id>/` | Raw JSON for a run |
| [`scripts/capacity/README.md`](../../scripts/capacity/README.md) | Operator harness notes |
