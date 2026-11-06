# Ledger observability and initial SLOs

These are initial engineering SLOs for the ledger plane. They are not customer
banking SLAs and must be recalibrated from production traffic.

## Signals

Prometheus metrics are exposed at `/q/metrics`. The application records
histograms for:

- `jblock.transfer.accept`
- `jblock.funding`
- `jblock.settlement`

Quarkus OpenTelemetry instruments HTTP and JDBC. Production exports traces to
the required `OTEL_EXPORTER_OTLP_ENDPOINT`; production startup must provide a
reachable collector URL. Logs include trace and span IDs. Dashboards must
separate transfer acceptance from batch settlement.

## Initial objectives

- Transfer acceptance availability: at least 99.9% successful responses,
  excluding client validation, duplicate idempotency, and velocity-limit
  responses.
- Transfer acceptance latency: p95 below 100 ms under the proven host envelope.
- Funding availability: at least 99.9%, with every successful credit linked to
  one idempotency record.
- Settlement freshness: 99% of accepted transfers settled within the selected
  batch interval plus 60 seconds.
- Ledger correctness: zero duplicate settled transaction IDs, zero negative
  materialized balances, and zero unexplained reconciliation differences.
- Recovery: PostgreSQL restore drill succeeds and measured RPO/RTO stay within
  the managed-service policy.

## Required alerts

Alert on sustained HTTP 5xx, acceptance p95 breach, oldest pending transaction,
pool depth, settlement duration/failure, PostgreSQL advisory-lock contention,
duplicate transaction-fence violations, negative-balance rollback, Kafka
consumer lag, datasource exhaustion, disk pressure, backup failure, and restore
drill failure.

## Capacity gate

Run the capacity battery with authentication enabled before each release that
changes signing, persistence, pool, settlement, or security. Record accepted
transactions per second separately from settled transactions per second and
settlement freshness. A release fails the gate if correctness breaks even when
throughput remains above target.
