# Capacity baseline

Host-specific Phase-1 numbers from the **full** local battery run on **2026-11-06**.

| Field | Value |
|-------|-------|
| Canonical evidence | [`evidence/20261106T185800Z/`](evidence/20261106T185800Z/) |
| Earlier money-only (string amounts) | [`evidence/20261106T185200Z/`](evidence/20261106T185200Z/) |
| Earlier B/C/smoke | [`evidence/20261106T183500Z/`](evidence/20261106T183500Z/) |
| Machine | 13th Gen Intel i7-1355U, 12 threads, ~30 GiB RAM (`machine.json`) |
| App | Root Quarkus JVM `:8018` + Compose Mongo 7 + Kafka 3.8 |

How to read these numbers: [README.md](README.md). Where limits sit: [LIMITATIONS.md](LIMITATIONS.md).

## Suite A — Money

| Metric | Last-good | First-fail |
|--------|-----------|------------|
| Single transfer amount (ledger `$`, amount as JSON **string**) | `9223372036854775807` (`Long.MAX_VALUE`) | *none in probe ladder* |
| Settled volume (50 × chunk 100) | `5000` (all chunks OK) | — |

Notes:

- Probe ladder reached `Long.MAX_VALUE` successfully when `amount` is a JSON string (Jackson → `BigDecimal`).
- Sending `amount` as a JSON **number** is unsafe above ~`2^53−1` (IEEE-754). See LIMITATIONS.
- Historical `MoneyFlowTest` note (“Kafka error above ~10000”) was **not** reproduced for single-transfer settlement up to `Long.MAX_VALUE` on this host/run.
- Latent storage bound remains `WalletCodec` decode via `longValue()` (~`Long.MAX_VALUE`) if balances larger than signed long are ever introduced.

## Suite B — Wallets

| Metric | Last-good | First-fail / stop |
|--------|-----------|-------------------|
| Wallet count | **2000** | soft stop `hard_cap` (no HTTP/ready failure) |
| Create latency | p50 ≈ 11 ms, p95 ≈ 18 ms, p99 ≈ 32 ms | — |
| Stop reason | `hard_cap` (budget 2000 / 480 s) | — |

No hard cliff inside the configured cap. Re-run with a higher `--wallet-hard-cap` to hunt further.

## Suite C — Throughput

| Metric | Value |
|--------|-------|
| tx/s | **≈ 12.06** |
| `$`/s settled | **≈ 12.06** (1 unit/tx) |
| mine cycles/s | **≈ 0.76** |
| Window | 90 s, 16 funded pairs, 8 workers |
| tx_ok / tx_fail | 1309 / 19 (error rate ≈ 1.4%) |
| Stop reason | `duration` |
| tx latency | p50 ≈ 28 ms, p95 ≈ 53 ms, p99 ≈ 57 ms |

An earlier concurrent run (`20261106T183209Z`) also surfaced `ConcurrentModificationException` on mine under load — see LIMITATIONS (source).

## Smoke post-check

| Check | Result |
|-------|--------|
| `smoke-two-nodes.sh` | **OK** (peer tip matched root after capacity suites) |
