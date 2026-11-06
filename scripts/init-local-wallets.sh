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
MONGO_URI="${MONGO_URI:-mongodb://localhost:27017}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
API_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
OUT_FILE="${INIT_WALLETS_FILE:-/tmp/jblock-local-wallets.json}"

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

echo "==> Seed alice (10000) + bob (1000) into Mongo"
cd "${API_DIR}"
WALLETS_JSON=$(./mvnw -q test-compile exec:java \
  -Dexec.classpathScope=test \
  -Dexec.mainClass=api.network.InitTestWallets \
  -Dquarkus.mongodb.connection-string="${MONGO_URI}")
echo "${WALLETS_JSON}" > "${OUT_FILE}"

ALICE_UID=$(json_get "${WALLETS_JSON}" "d['alice']['uid']")
ALICE_ADDR=$(json_get "${WALLETS_JSON}" "d['alice']['address']")
ALICE_BAL=$(json_get "${WALLETS_JSON}" "d['alice']['balance']")
BOB_UID=$(json_get "${WALLETS_JSON}" "d['bob']['uid']")
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
   -H 'Content-Type: application/json' \\
   -d '{"uid":"${ALICE_UID}","recipient":"${BOB_ADDR}","amount":20,"type":"WALLET"}'

 curl -s -X POST ${ROOT}/M1n3/7r4N54C710nZ | python3 -m json.tool | tail

 curl -s -X POST ${ROOT}/CrYP70/Ch41N | python3 -c \\
   "import sys,json; c=json.load(sys.stdin); print('blocks',len(c),'tip',c[-1]['hash'][:16]+'…')"

 If PoolResource logs "wrong usage", the body is missing uid/recipient/amount/type.
============================================================
EOF
