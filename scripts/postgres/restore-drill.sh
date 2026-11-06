#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "Usage: $0 <backup.dump>" >&2
  exit 2
fi

CONTAINER="${POSTGRES_CONTAINER:-java-blockchain-api-postgres-1}"
USER="${POSTGRES_USER:-blockchain}"
DRILL_DB="${POSTGRES_DRILL_DB:-blockchain_restore_drill}"
BACKUP="$1"

cat "${BACKUP}" | docker exec -i "${CONTAINER}" sh -c \
  "dropdb --if-exists -U '${USER}' '${DRILL_DB}' &&
   createdb -U '${USER}' '${DRILL_DB}' &&
   pg_restore -U '${USER}' -d '${DRILL_DB}' --clean --if-exists"

docker exec "${CONTAINER}" psql -U "${USER}" -d "${DRILL_DB}" \
  -v ON_ERROR_STOP=1 \
  -c "SELECT count(*) AS wallet_count FROM ledger_wallet;" \
  -c "SELECT count(*) AS block_count FROM ledger_block;"

echo "Restore drill passed in database ${DRILL_DB}"
