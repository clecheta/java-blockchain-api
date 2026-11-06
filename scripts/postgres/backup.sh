#!/usr/bin/env bash
set -euo pipefail

CONTAINER="${POSTGRES_CONTAINER:-java-blockchain-api-postgres-1}"
DATABASE="${POSTGRES_DB:-blockchain}"
USER="${POSTGRES_USER:-blockchain}"
OUTPUT="${1:-ledger-$(date -u +%Y%m%dT%H%M%SZ).dump}"

docker exec "${CONTAINER}" pg_dump \
  --username="${USER}" --dbname="${DATABASE}" --format=custom > "${OUTPUT}"

echo "Backup written: ${OUTPUT}"
