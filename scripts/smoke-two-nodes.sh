#!/usr/bin/env bash
# Full HTTP multi-node smoke (no UI) — api#41 alternative A / best cost-benefit for api#40.
#
# Prerequisites:
#   docker-compose up -d          # Kafka
#   MongoDB on mongodb://localhost:27017
#   Root:  ./mvnw quarkus:dev                          # :8018 group 18
#   Peer:  ./mvnw quarkus:dev -Dquarkus.profile=node   # :8081 group 81
#
# What it does:
#   1) wait health on both nodes
#   2) seed funded src wallet in Mongo (HTTP create is always balance 0)
#   3) create dest wallet via HTTP
#   4) pool tx + mine on root
#   5) assert peer cryptochain tip advanced and tip hash matches root
#
set -euo pipefail

ROOT="${ROOT_URL:-http://localhost:8018}"
PEER="${PEER_URL:-http://localhost:8081}"
MONGO_URI="${MONGO_URI:-mongodb://localhost:27017}"
AMOUNT="${SMOKE_AMOUNT:-20}"
CLIENT_KEY="${BANK_CLIENT_API_KEY:-local-client-only-change-me}"
SERVICE_KEY="${BANK_SERVICE_API_KEY:-local-development-only-change-me}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
API_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

json_field() {
  local json="$1" field="$2"
  python3 -c "import sys,json; d=json.loads(sys.argv[1]); print(d['${field}'])" "${json}"
}

chain_len() {
  local url="$1"
  curl -sf -X POST "${url}/CrYP70/Ch41N" -H "X-API-Key: ${CLIENT_KEY}" | python3 -c "import sys,json; print(len(json.load(sys.stdin)))"
}

tip_hash() {
  local url="$1"
  curl -sf -X POST "${url}/CrYP70/Ch41N" -H "X-API-Key: ${CLIENT_KEY}" | python3 -c "import sys,json; c=json.load(sys.stdin); print(c[-1]['hash'] if c else '')"
}

wait_health() {
  local url="$1" name="$2"
  echo "==> Waiting for ${name} health (${url}/q/health/ready)"
  for i in $(seq 1 60); do
    if curl -sf "${url}/q/health/ready" >/dev/null; then
      echo "    ${name} ready"
      return 0
    fi
    sleep 2
  done
  echo "FAIL: ${name} not ready"
  exit 1
}

echo "==> Multi-node HTTP smoke (root=${ROOT} peer=${PEER})"
wait_health "${ROOT}" "root"
wait_health "${PEER}" "peer"

echo "==> Create and fund source wallet"
cd "${API_DIR}"
SRC_JSON=$(curl -sf -X POST "${ROOT}/W4LL37/cRE47E" \
  -H "X-API-Key: ${CLIENT_KEY}" \
  -H 'Content-Type: application/json' \
  -d "smoke-source-$(date +%s)-abcdefgh")
SRC_UID=$(json_field "${SRC_JSON}" uid)
FUND_KEY="smoke-fund-$(date +%s%N)"
curl -sf -X POST "${ROOT}/W4LL37/fUND" \
  -H "X-API-Key: ${SERVICE_KEY}" \
  -H 'Content-Type: application/json' \
  -d "{\"uid\":\"${SRC_UID}\",\"amount\":\"10000\",\"idempotencyKey\":\"${FUND_KEY}\"}" >/dev/null
echo "    src=${SRC_JSON}"
SRC_ADDR=$(json_field "${SRC_JSON}" address)

echo "==> Create destination wallet (HTTP)"
DEST_JSON=$(curl -sf -X POST "${ROOT}/W4LL37/cRE47E" \
  -H "X-API-Key: ${CLIENT_KEY}" \
  -H 'Content-Type: application/json' \
  -d "\"smoke-dest-$(date +%s)-abcdefgh\"")
echo "    dest=${DEST_JSON}"
DEST_ADDR=$(json_field "${DEST_JSON}" address)

BEFORE=$(chain_len "${PEER}")
echo "==> Peer cryptochain size before: ${BEFORE}"

echo "==> Transact ${AMOUNT} from src → dest (root pool)"
TX_BODY=$(python3 -c "import json; print(json.dumps({
  'uid': '''${SRC_UID}''',
  'recipient': '''${DEST_ADDR}''',
  'amount': '''${AMOUNT}''',
  'type': 'WALLET'
}))")
curl -sf -X POST "${ROOT}/p00L/tRaN5ACt" \
  -H "X-API-Key: ${CLIENT_KEY}" \
  -H "Idempotency-Key: smoke-tx-$(date +%s%N)" \
  -H 'Content-Type: application/json' \
  -d "${TX_BODY}" >/tmp/smoke-tx.json
echo "    tx ok"

echo "==> Mine on root"
curl -sf -X POST "${ROOT}/M1n3/7r4N54C710nZ" \
  -H "X-API-Key: ${SERVICE_KEY}" -o /tmp/smoke-mine.json
ROOT_TIP=$(python3 -c "import json; r=json.load(open('/tmp/smoke-mine.json')); print(r['blockHash'])")
echo "    root tip hash=${ROOT_TIP}"

echo "==> Wait for Kafka peer sync"
SYNC_OK=0
AFTER="${BEFORE}"
for i in $(seq 1 30); do
  AFTER=$(chain_len "${PEER}")
  PEER_TIP=$(tip_hash "${PEER}")
  if [[ "${AFTER}" -gt "${BEFORE}" && "${PEER_TIP}" == "${ROOT_TIP}" ]]; then
    SYNC_OK=1
    break
  fi
  sleep 1
done

echo "==> Peer cryptochain size after: ${AFTER}"
echo "    peer tip hash=$(tip_hash "${PEER}")"

if [[ "${SYNC_OK}" -ne 1 ]]; then
  echo "FAIL: peer did not sync tip (before=${BEFORE} after=${AFTER} root_tip=${ROOT_TIP})"
  exit 1
fi

echo "OK: peer advanced and tip hash matches root (src=${SRC_ADDR:0:16}… → dest=${DEST_ADDR:0:16}…)"
