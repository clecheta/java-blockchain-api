# PostgreSQL ledger runbook

PostgreSQL is the queryable ledger source of truth. Kafka remains the
messaging/fan-out plane.

## Local start

```bash
docker compose up -d postgres kafka
./mvnw compile quarkus:dev
```

Flyway applies the ledger and settlement-fence migrations; Hibernate validates
the schema.

## Backup and restore drill

```bash
scripts/postgres/backup.sh /tmp/jblock-ledger.dump
scripts/postgres/restore-drill.sh /tmp/jblock-ledger.dump
```

For managed AWS/GCP/Azure PostgreSQL, enable continuous backups and PITR in
cloud IaC. The scripts are the portable local/drill baseline, not a substitute
for managed retention policies.

## Settlement fencing

Only the settler role may mine (`ledger.node.role=settler` and
`mining.settlement.enabled=true`). Peer profiles keep settlement disabled.
Cloud schedulers should use concurrency=1; PostgreSQL advisory locking is the
correctness boundary across processes.
