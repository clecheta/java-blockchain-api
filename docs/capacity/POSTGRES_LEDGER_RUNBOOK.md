# PostgreSQL ledger runbook

PostgreSQL is the queryable ledger source of truth. Kafka remains the
messaging/fan-out plane.

## Local start

```bash
docker compose up -d postgres kafka mongo
./mvnw compile quarkus:dev
```

Flyway applies the ledger and settlement-fence migrations; Hibernate validates
the schema.
Mongo remains in Compose only for the controlled backfill window.

## Mongo backfill

1. Take Mongo and PostgreSQL backups.
2. Start one settlement node with
   `LEDGER_MIGRATION_MONGO_ENABLED=true`.
3. Verify logged wallet/block counts and compare chain tips.
4. Restart with migration disabled.
5. Run the capacity battery and two-node smoke.

The backfill is idempotent by wallet UID hash and block hash.

## Backup and restore drill

```bash
scripts/postgres/backup.sh /tmp/jblock-ledger.dump
scripts/postgres/restore-drill.sh /tmp/jblock-ledger.dump
```

For managed AWS/GCP/Azure PostgreSQL, enable continuous backups and PITR in
cloud IaC. The scripts are the portable local/drill baseline, not a substitute
for managed retention policies.

## Settlement fencing

Settlement uses:

1. Root/settlement role configuration.
2. JVM single-flight lock.
3. `pg_try_advisory_xact_lock` for cross-process fencing.
4. A unique durable transaction-ID fence.
5. Cloud scheduler concurrency set to one.

Peers never schedule or execute settlement.

Provider-specific setup and two-node safety:
[Multi-cloud settlement scheduling](CLOUD_SETTLEMENT_SCHEDULING.md).
