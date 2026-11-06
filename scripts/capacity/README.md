# JBlock API capacity battery (Phase 1) — operator notes

**Human guide (why / how to run / how to evaluate):** [`docs/capacity/README.md`](../../docs/capacity/README.md)

Local ceiling hunt for ledger `$` volume, wallet count, and host throughput.
Results classify each cliff as **architecture**, **source code**, or **infrastructure**.

Phase 2 (JMeter/k6 sustained load) is out of scope here — see GitLab api#20 / api#43.

## Prerequisites

1. Java 25 + Maven wrapper (project `./mvnw`)
2. Docker Compose: Mongo + Kafka — `docker compose up -d` from repo root
3. Root API on `:8018` — `./mvnw compile quarkus:dev`
4. Python 3.10+ with deps (prefer `uv` if `python3-venv` / `pip` are missing):

```bash
uv venv .venv-capacity
uv pip install --python .venv-capacity/bin/python -r scripts/capacity/requirements.txt
# or: python3 -m venv .venv-capacity && pip install -r scripts/capacity/requirements.txt
```

Sibling `java-blockchain` must already be `mvn install`-ed (same as normal local test).

**Important:** `amount` in `POST /p00L/tRaN5ACt` must be a JSON **string** for values above ~`2^53−1`. The harness does this automatically.

## Run

```bash
# from java-blockchain-api/
.venv-capacity/bin/python scripts/capacity/run_battery.py
```

Useful flags:

| Flag | Default | Meaning |
|------|---------|---------|
| `--base-url` | `http://localhost:8018` | Root API |
| `--mongo-uri` | `mongodb://localhost:27017` | Funded-wallet seeder |
| `--wallet-hard-cap` | `5000` | Suite B create ceiling |
| `--wallet-time-budget-s` | `600` | Suite B soft stop |
| `--throughput-duration-s` | `90` | Suite C window |
| `--throughput-pairs` | `20` | Concurrent funded senders |
| `--skip-smoke` | off | Skip `smoke-two-nodes.sh` post-check |
| `--skip-money` / `--skip-wallets` / `--skip-throughput` | off | Skip a suite |

Faster local dry-run:

```bash
python3 scripts/capacity/run_battery.py \
  --wallet-hard-cap 200 --wallet-time-budget-s 120 \
  --throughput-duration-s 45 --throughput-pairs 10 \
  --skip-smoke
```

## Evidence

Written under `docs/capacity/evidence/<run-id>/`:

- `machine.json` — host fingerprint
- `suite_money.json` / `suite_wallets.json` / `suite_throughput.json`
- `limitations.json` — tagged cliffs (architecture / source_code / infrastructure)
- `summary.json` — short numeric rollup
- `smoke_two_nodes.json` — multi-node post-check (unless `--skip-smoke`)

Human reports (filled after a run):

- [`docs/capacity/CAPACITY_BASELINE.md`](../../docs/capacity/CAPACITY_BASELINE.md) — numbers
- [`docs/capacity/LIMITATIONS.md`](../../docs/capacity/LIMITATIONS.md) — where limits sit

## Suites

| Suite | Question | Method |
|-------|----------|--------|
| A — money | Max single transfer + settled volume | Seed treasury → transact → mine → balance check; geometric + binary search |
| B — wallets | Max accounts | Parallel `POST /W4LL37/cRE47E` batches |
| C — throughput | Max this host can process | N funded senders, concurrent transfers, mine every K txs |

Funding uses `api.network.CapacityWalletSeeder` (test classpath) because HTTP create always starts at balance **0**.
