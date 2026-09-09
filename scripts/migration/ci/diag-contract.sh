#!/usr/bin/env bash
# Bitta shartnoma tashxisi (READ-ONLY). Commit sarlavhasida num=<raqam> bo'lsa o'sha.
set -uo pipefail
NUM="${FIX_NUM:-4706}"
if echo "${CI_COMMIT_TITLE:-}" | grep -qE 'num=[0-9]+'; then
  NUM="$(echo "${CI_COMMIT_TITLE}" | grep -oE 'num=[0-9]+' | head -1 | cut -d= -f2)"
fi
echo "DIAG NUM=$NUM"
NU="$(docker exec main-service printenv POSTGRES_USER 2>/dev/null || echo postgres)"
NP="$(docker exec main-service printenv POSTGRES_PASSWORD 2>/dev/null || true)"
docker run --rm -i --network hesap-postgres -v "$CI_PROJECT_DIR/scripts/migration":/sql \
  -e PGPASSWORD="$NP" postgres:16-alpine \
  psql -h postgres -p 5435 -U "$NU" -d hesap -v ON_ERROR_STOP=1 -v num="$NUM" \
  -f /sql/diag-contract.sql
