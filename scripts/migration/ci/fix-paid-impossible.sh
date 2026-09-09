#!/usr/bin/env bash
# paid_amount ortiqcha x100 tuzatish. [apply] sarlavhada bo'lsa yozadi.
set -uo pipefail
APPLY=false
echo "${CI_COMMIT_TITLE:-}" | grep -q '\[apply\]' && APPLY=true
echo "APPLY=$APPLY"
NU="$(docker exec main-service printenv POSTGRES_USER 2>/dev/null || echo postgres)"
NP="$(docker exec main-service printenv POSTGRES_PASSWORD 2>/dev/null || true)"
docker run --rm -i --network hesap-postgres -v "$CI_PROJECT_DIR/scripts/migration":/sql \
  -e PGPASSWORD="$NP" postgres:16-alpine \
  psql -h postgres -p 5435 -U "$NU" -d hesap -v ON_ERROR_STOP=1 -v apply="$APPLY" -f /sql/16-fix-paid-impossible.sql
