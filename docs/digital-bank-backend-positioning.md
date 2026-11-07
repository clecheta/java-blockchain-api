# JBlock API as a digital bank backend?

Short positioning note: what this project already covers relative to a digital-bank backend, what it does not, and how to treat it honestly in product or architecture discussions.

Related reading: [Public vs private blockchain and JBlock positioning](./public-vs-private-posicionamento-jblock.md).

---

## Verdict

**Partially — as a ledger / settlement core prototype, not as a ready digital-bank backend.**

`java-blockchain-api` (JBlock API) implements wallets, signed transfers, a pending pool, block confirmation (mining), an append-only chain, and multi-node sync. That overlaps the **accounts + movements + ledger** slice of a bank. It does **not** implement identity, compliance, fiat rails, regulated custody, or banking products.

Treat it as a **cryptochain / shared-ledger lab and MVP stack**, or as a possible **internal ledger engine** inside a larger fintech. Do not treat it as a drop-in core banking system or a regulated digital bank.

---

## What already looks like “bank plumbing”

| Bank-like concern | How it appears here |
|---|---|
| Accounts | ECDSA wallets (`WalletResource` — create / info) |
| Balance | PostgreSQL materialized balance plus append-only settlement history |
| Transfers | Wallet → wallet via transaction pool (`PoolResource`) |
| Settlement / confirmation | Scheduled, single-writer batch settlement (`MineResource`) |
| Immutable book | Cryptochain blocks persisted in PostgreSQL and fanned out through Kafka |
| HTTP surface | Quarkus REST API + demo UI |

Operationally the network is **closed / private**: whoever controls PostgreSQL,
Kafka, credentials, and the network perimeter controls who participates. Trust
comes from infrastructure isolation, API keys, and ECDSA signatures — not from
banking KYC or customer-grade authorization.

Historical project language once described the chain wallet as a “bank account,” then moved toward **individual wallets** for wallet-to-wallet flows. That metaphor is useful for teaching; it is not a banking domain model.

---

## What a digital bank backend needs that this project does not provide

| Typical digital bank | This project today |
|---|---|
| Login, session, customer authorization | Bootstrap client/service API keys only; no OIDC, customer identity, or consent model |
| KYC / AML, limits, sanctions screening | Not present |
| Pix / TED / cards / credit / fees / statements | On-chain transfers + optional mining reward only (`PayTypeEnum` labels are decorative types, not payment rails) |
| Regulated custody, audit trails for supervisors, chargebacks / disputes | PoW cryptochain model, not accounting/custody ops |
| Production SLAs, formal change control, dual control | Capacity harness, metrics, and scheduled settlement exist; formal SLO operations and dual control do not |

Open Expansion items that matter if anyone stretches toward “bank-like” use (auth, balance calculation hardening, when to mine, datastore decision) live in the sibling lineage docs under `java-blockchain-docs` — they do not close the gap to a regulated bank by themselves.

---

## Honest ways to use this positioning

**Appropriate**

- Teaching / portfolio narrative: “ledger + wallets + multi-node sync in Java.”
- Sandbox for internal signed value transfer between controlled parties.
- Prototype of a **ledger plane** that a separate identity, compliance, and payments stack could sit in front of.
- Embeddable domain via the `java-blockchain` library inside another Java service (still not “a bank”).

**Inappropriate**

- Claiming JBlock API *is* a digital-bank backend or core banking replacement.
- Shipping customer funds or regulated deposits on this stack as-is.
- Equating PoW mining rewards with interest, issuance policy, or central-bank money.

---

## If the goal were a fintech ledger layer (not a bank)

Without abandoning the current codebase, natural next steps toward a **stronger ledger service** (still short of a digital bank) would include:

1. Replace bootstrap API keys with customer/service identity (OIDC, mTLS, scoped roles, and node allowlists).
2. Hardening balance and starting-balance rules so configuration cannot inflate spendable funds.
3. Clear custody model (who holds keys; hot vs cold; no demo `uid` pasted into a browser).
4. Operate the existing scheduled settlement policy with explicit latency SLOs,
   multi-cloud failover, and reward policy disabled.
5. Complete the PostgreSQL HA/PITR, immutable audit, reconciliation, and
   operational-control story.

A **regulated digital bank** would still require a separate product and compliance stack around (or instead of) this ledger: customer identity, payment schemes, risk, reporting, and legal entity controls.

---

## Capacity evidence (local ledger plane)

Start with the [capacity testing guide](./capacity/README.md) (why, how to run, how to evaluate). Measured ceilings: [CAPACITY_BASELINE.md](./capacity/CAPACITY_BASELINE.md). Where limits sit (architecture vs source vs infrastructure): [LIMITATIONS.md](./capacity/LIMITATIONS.md). Harness: `scripts/capacity/` (Phase 1). This does **not** make the API a digital bank; it bounds the ledger plane on a given machine.

The escalated 2026-11-07 secured battery passed G1 with a **100,000**-wallet
create sweep and a best local accepted throughput of **511.985**
transactions/s (prior secured comparable: 243.961 tx/s). Durable PostgreSQL
pending reservations and shared spend fencing are in place; same-wallet and
two-node accept suites pass without negative balances. It still does **not**
meet the 1,000 transactions/s G2 target on this laptop. Remaining ledger
pressure is hot-path CPU/DB work and managed-environment scale/PITR, not
missing cross-node reservation.

The path from that baseline toward market-grade ledger infrastructure is
documented in the
[capacity improvement roadmap](./capacity/IMPROVEMENT_ROADMAP.md).

### Account-scale success benchmark

For “is this ledger plane successful enough to back a real digital-bank product?”,
use a **mid-tier Brazilian digital-bank account scale** as the external
reference — not a laptop hard-cap and not a Big Four retail franchise.

Public reporting for that peer class is on the order of **tens of millions of
customers / opened accounts**, with recent figures clustered around
**~35–40 million**. Treat **40 million ledger accounts** as the headline
success target for this API’s account plane:

| Milestone | Account count | Meaning |
|-----------|---------------|---------|
| Proven locally (G1+) | 100,000 | Hard-cap create sweep on one laptop |
| Roadmap G2/G3 design | 100,000 measured → 1,000,000 design | Engineering gates, not market parity |
| **Market success bar** | **~40,000,000** | Mid-tier digital-bank account scale; treat the ledger as competitive only when this order of magnitude is credible under production SLOs |

That bar is about **durable account storage, create/read latency, backup/PITR,
and multi-node correctness** at tens of millions of rows — not about claiming
the API is a bank. Throughput gates (accepted transactions/s) remain separate;
hitting 40 million dormant rows without healthy transfer and settlement
capacity is still a failed ledger plane.

## Synthesis

| Question | Answer |
|---|---|
| Overlap with a digital bank? | Yes — accounts, transfers, ledger. |
| Is it a digital-bank backend? | **No.** |
| Best framing? | Private, Java-owned **ledger / cryptochain** prototype or MVP engine. |
| Account success bar? | **~40 million** accounts (mid-tier digital-bank scale); local proof today is 100,000. |
| Path upward? | Harden as an **internal ledger plane**; build bank/fintech product concerns elsewhere. |
