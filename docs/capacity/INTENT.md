# Capacity battery — intent

Canonical guide: [README.md](README.md) (why, how to run, how to evaluate).

## Why

We want measured ceilings for treating JBlock API as a **private ledger / settlement plane** (not a regulated digital bank). With PostgreSQL as the locked ledger SoT ([api#43](https://gitlab.com/cleberlecheta/java-blockchain-api/-/issues/43)), we need evidence for:

1. Maximum ledger `$` volume the API can settle on this host (ceiling hunt).
2. Maximum wallet/account count (ceiling hunt).
3. Maximum processing rate this machine can sustain (tx/s, mine cycles/s, `$`/s).
4. The required account-volume stages, finished locally: 5M accounts with 150 billion moved (**done** — `evidence/volume-v5-20261107T182239Z/`), then 10M/300 billion, 50M/600 billion, 100M/1.1 trillion. Spec: [SPEC.md](SPEC.md).

## Success looks like

- A reproducible Phase-1 harness under `scripts/capacity/`.
- Evidence under `docs/capacity/evidence/<run-id>/`.
- Human-readable **numbers** in `CAPACITY_BASELINE.md` and **where limits sit** in `LIMITATIONS.md` (architecture vs source code vs infrastructure).
- Each volume stage run until both the account count and the moved volume are reached. Duration and 90–100% hardware use are allowed. Stopping short is a failure. One JVM, extra local nodes, and JMeter (or k6 / Gatling) are all valid drivers; PostgreSQL stays the source of truth.

## Non-goals

- Claiming digital-bank readiness.
- Re-opening the datastore SoT decision (locked on PostgreSQL in api#43).
- Treating a JMeter RPS report as a substitute for the fund → transact → settle balance check.
- Waiving a volume stage because the local machine is slow or saturated.

## Related

- GitLab [api#20](https://gitlab.com/cleberlecheta/java-blockchain-api/-/issues/20) (stress intent) → superseded path [api#43](https://gitlab.com/cleberlecheta/java-blockchain-api/-/issues/43)
