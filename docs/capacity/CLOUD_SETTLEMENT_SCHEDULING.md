# Multi-cloud settlement scheduling

JBlock accepts transfers continuously and settles them in batches. Production
must invoke one authenticated endpoint:

```text
POST /M1n3/7r4N54C710nZ
X-API-Key: <service credential>
```

The interval and `mining.settlement.max-batch-size` are operating policies, not
ledger correctness mechanisms. Choose them from the target settlement-latency
SLO, pool depth, and measured settlement throughput. A `200` response contains
the block hash, height, and transaction count. A `204`
response means another writer holds the lease or there was nothing to settle;
it is safe and should not be retried immediately.

## Required application configuration

Run API replicas with `ledger.node.role=peer` and
`mining.settlement.enabled=false`. Run the settlement deployment with
`ledger.node.role=settler` and `mining.settlement.enabled=true`. Keep
`mining.settlement.cron=disabled` when a managed scheduler is active.

Expose the settlement deployment only on a private network or authenticated
gateway. Store `bank.service.api-key` in the cloud secret manager and rotate it;
never put the local default in scheduler configuration.

## Cloud mappings

### AWS

Use EventBridge Scheduler to invoke a Lambda or private ECS task that performs
the HTTPS POST. Set Lambda reserved concurrency to `1` (or ECS desired task
count to `1`), disable flexible time windows, and source the service credential
from Secrets Manager. An EventBridge scheduled rule with an API Destination is
also valid when the endpoint is gateway-accessible.

### GCP

Use Cloud Scheduler with an HTTP target to a private Cloud Run settlement
service. Use OIDC for the platform call, keep the JBlock service credential in
Secret Manager, and configure the settlement service with maximum instances
`1` and concurrency `1`.

### Azure

Use a Logic Apps recurrence with an HTTP action, or a timer-triggered Function
that calls the private settlement service. Store the credential in Key Vault.
For Functions, cap the settlement worker scale-out to one; for Logic Apps,
enable recurrence concurrency control with degree `1`.

## Why two or more nodes remain safe

Cloud concurrency limits reduce noise but are not the correctness boundary.
Every settlement request is fenced in this order:

1. Peer replicas return `403`; only the settlement role can execute.
2. A JVM lock prevents overlap inside one process.
3. `pg_try_advisory_xact_lock` elects one writer across processes and clouds.
4. The writer reloads the PostgreSQL ledger before constructing a block.
5. `ledger_settled_transaction.transaction_id` is unique, preventing a
   transaction seen by multiple nodes from settling twice.
6. Block persistence, transaction fencing, and materialized balance updates
   commit in one database transaction. Pool removal and Kafka fan-out happen
   only after commit.

This makes duplicate ticks and simultaneous requests safe. It does not make a
single PostgreSQL primary multi-region: cross-region disaster recovery,
promotion, and scheduler failover must follow the managed database runbook.
Never enable schedulers in two regions until the promoted database is writable
and the old primary is fenced.

## Verification

Before production:

1. Run `SettlementLeaseTest` and `PeerMineResourceTest`.
2. Send simultaneous settlement requests to at least two replicas.
3. Verify one block height, one row per transaction in
   `ledger_settled_transaction`, and no negative wallet balances.
4. Stop the active settler, promote the standby path, and repeat.
5. Alert on settlement failures, pool age/depth, duplicate-key violations,
   balance-update rollback, and scheduler delivery failures.
