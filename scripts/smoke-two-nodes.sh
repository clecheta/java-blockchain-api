#!/usr/bin/env bash
# Smoke: assume Kafka + root (:8018) + node (:8081) are already up.
# Mines on root (after ensuring a pooled tx exists is caller's job) and checks peer tip grew.
#
# Usage:
#   docker-compose up -d
#   ./mvnw quarkus:dev &
#   ./mvnw quarkus:dev -Dquarkus.profile=node -Ddebug=5006 &
#   # create wallets + pool tx via API, then:
#   ./scripts/smoke-two-nodes.sh
set -euo pipefail

ROOT="${ROOT_URL:-http://localhost:8018}"
PEER="${PEER_URL:-http://localhost:8081}"

echo "==> Fetch peer cryptochain size (before)"
BEFORE=$(curl -s -X POST "${PEER}/CrYP70/Ch41N" | python3 -c "import sys,json; print(len(json.load(sys.stdin)))")
echo "peer blocks before: ${BEFORE}"

echo "==> Mine on root"
curl -s -X POST "${ROOT}/M1n3/7r4N54C710nZ" -o /tmp/mine-root.json || {
  echo "Mine failed — ensure transaction pool has valid txs on root"
  exit 1
}

echo "==> Wait for Kafka sync"
sleep 3

echo "==> Fetch peer cryptochain size (after)"
AFTER=$(curl -s -X POST "${PEER}/CrYP70/Ch41N" | python3 -c "import sys,json; print(len(json.load(sys.stdin)))")
echo "peer blocks after: ${AFTER}"

if [[ "${AFTER}" -le "${BEFORE}" ]]; then
  echo "FAIL: peer tip did not advance (before=${BEFORE} after=${AFTER})"
  exit 1
fi

echo "OK: peer advanced by $((AFTER - BEFORE)) block(s)"
