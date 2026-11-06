#!/usr/bin/env bash
# Seed funded local wallets and print exactly what to paste where.
#
# Prerequisites:
#   docker compose up -d
#   Root node up: ./mvnw compile quarkus:dev   # :8018
#
# Usage:
#   ./scripts/init-local-wallets.sh
#
set -euo pipefail

ROOT="${ROOT_URL:-http://localhost:8018}"
OUT_FILE="${INIT_WALLETS_FILE:-/tmp/jblock-local-wallets.json}"
CLIENT_KEY="${BANK_CLIENT_API_KEY:-local-client-only-change-me}"
SERVICE_KEY="${BANK_SERVICE_API_KEY:-local-development-only-change-me}"

json_get() {
  local json="$1" expr="$2"
  python3 -c "import sys,json; d=json.loads(sys.argv[1]); print(${expr})" "${json}"
}

wait_health() {
  local url="$1"
  echo "==> Waiting for root health (${url}/q/health/ready)"
  for _ in $(seq 1 60); do
    if curl -sf "${url}/q/health/ready" >/dev/null; then
      echo "    root ready"
      return 0
    fi
    sleep 2
  done
  echo "FAIL: root not ready at ${url}"
  exit 1
}

echo "==> Init local wallets (root=${ROOT})"
wait_health "${ROOT}"

echo "==> Create and fund alice (10000) + bob (1000)"
ALICE_JSON=$(curl -sf -X POST "${ROOT}/W4LL37/cRE47E" \
  -H "X-API-Key: ${CLIENT_KEY}" -H 'Content-Type: application/json' \
  -d "alice-local-$(date +%s%N)")
BOB_JSON=$(curl -sf -X POST "${ROOT}/W4LL37/cRE47E" \
  -H "X-API-Key: ${CLIENT_KEY}" -H 'Content-Type: application/json' \
  -d "bob-local-$(date +%s%N)")
ALICE_UID=$(json_get "${ALICE_JSON}" "d['uid']")
BOB_UID=$(json_get "${BOB_JSON}" "d['uid']")
curl -sf -X POST "${ROOT}/W4LL37/fUND" \
  -H "X-API-Key: ${SERVICE_KEY}" -H 'Content-Type: application/json' \
  -d "{\"uid\":\"${ALICE_UID}\",\"amount\":\"10000\",\"idempotencyKey\":\"alice-$(date +%s%N)\"}" >/dev/null
curl -sf -X POST "${ROOT}/W4LL37/fUND" \
  -H "X-API-Key: ${SERVICE_KEY}" -H 'Content-Type: application/json' \
  -d "{\"uid\":\"${BOB_UID}\",\"amount\":\"1000\",\"idempotencyKey\":\"bob-$(date +%s%N)\"}" >/dev/null
WALLETS_JSON=$(python3 -c 'import json,sys; a=json.loads(sys.argv[1]); b=json.loads(sys.argv[2]); a["balance"]="10000"; b["balance"]="1000"; print(json.dumps({"alice":a,"bob":b}))' "${ALICE_JSON}" "${BOB_JSON}")
echo "${WALLETS_JSON}" > "${OUT_FILE}"

ALICE_ADDR=$(json_get "${WALLETS_JSON}" "d['alice']['address']")
ALICE_BAL=$(json_get "${WALLETS_JSON}" "d['alice']['balance']")
BOB_ADDR=$(json_get "${WALLETS_JSON}" "d['bob']['address']")
BOB_BAL=$(json_get "${WALLETS_JSON}" "d['bob']['balance']")

cat <<EOF

============================================================
 Glossary (do not mix these up)
============================================================
 uid      = private wallet id of the SENDER (who pays)
 address  = public key of a wallet
 recipient= ALWAYS an address (public key), NEVER a uid
 type     = WALLET (fixed for these tests)

 Home page Address/Balance = miner info, IGNORE for transfers.

============================================================
 Accounts created (saved: ${OUT_FILE})
============================================================
 ALICE pays  (balance ${ALICE_BAL})
   uid:     ${ALICE_UID}
   address: ${ALICE_ADDR}

 BOB receives (balance ${BOB_BAL})
   uid:     ${BOB_UID}   ← not needed when Bob only receives
   address: ${BOB_ADDR}

============================================================
 Option A — browser  (${ROOT}/conduct-transaction)
============================================================
 Field "uid (sender)"       ← paste ALICE uid
 Field "recipient (address)"← paste BOB address
 Field "amount"             ← e.g. 20
 Submit → then open Transaction Pool → Mine Transactions

============================================================
 Option B — curl (same transfer)
============================================================
 curl -s -X POST ${ROOT}/p00L/tRaN5ACt \\
   -H 'X-API-Key: ${CLIENT_KEY}' \\
   -H 'Idempotency-Key: init-example-1' \\
   -H 'Content-Type: application/json' \\
   -d '{"uid":"${ALICE_UID}","recipient":"${BOB_ADDR}","amount":"20","type":"WALLET"}'

 curl -s -X POST ${ROOT}/M1n3/7r4N54C710nZ -H 'X-API-Key: ${SERVICE_KEY}'

 curl -s -X POST ${ROOT}/CrYP70/Ch41N -H 'X-API-Key: ${CLIENT_KEY}' | python3 -c \\
   "import sys,json; c=json.load(sys.stdin); print('blocks',len(c),'tip',c[-1]['hash'][:16]+'…')"

 If PoolResource logs "wrong usage", the body is missing uid/recipient/amount/type.
============================================================
EOF
