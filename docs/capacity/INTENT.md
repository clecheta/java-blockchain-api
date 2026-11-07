# Capacity battery — intent

Canonical guide: [README.md](README.md) (why, how to run, how to evaluate).

## Why

We want measured ceilings for treating JBlock API as a **private ledger / settlement plane** (not a regulated digital bank). Before datastore decisions ([api#43](https://gitlab.com/cleberlecheta/java-blockchain-api/-/issues/43)), we need evidence for:

1. Maximum ledger `$` volume the API can settle on this host.
2. Maximum wallet/account count.
3. Maximum processing rate this machine can sustain (tx/s, mine cycles/s, `$`/s).

## Success looks like

- A reproducible Phase-1 harness under `scripts/capacity/`.
- Evidence under `docs/capacity/evidence/<run-id>/`.
- Human-readable **numbers** in `CAPACITY_BASELINE.md` and **where limits sit** in `LIMITATIONS.md` (architecture vs source code vs infrastructure).

## Non-goals

- Claiming digital-bank readiness.
- Migrating Mongo → Postgres or rewriting codecs in this delivery.
- Phase-2 JMeter/k6 SLA reports (follows later at a known-safe load point).

## Related

- GitLab [api#20](https://gitlab.com/cleberlecheta/java-blockchain-api/-/issues/20) (stress intent) → superseded path [api#43](https://gitlab.com/cleberlecheta/java-blockchain-api/-/issues/43)
