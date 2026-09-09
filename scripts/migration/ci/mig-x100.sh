#!/usr/bin/env bash
# Eski (old hesap) shartnoma summalarini tiyinga (x100) o'tkazish.
# [apply] marker bo'lsa yozadi; aks holda faqat PREVIEW. Idempotent (marker jadval).
set -uo pipefail
APPLY=false
echo "${CI_COMMIT_TITLE:-}" | grep -q '\[apply\]' && APPLY=true
echo "APPLY=$APPLY"

NU="$(docker exec main-service printenv POSTGRES_USER 2>/dev/null || echo postgres)"
NP="$(docker exec main-service printenv POSTGRES_PASSWORD 2>/dev/null || true)"

docker run --rm --network hesap-postgres \
  -v "$CI_PROJECT_DIR/scripts/migration":/sql \
  -e PGPASSWORD="$NP" postgres:16-alpine \
  psql -h postgres -p 5435 -U "$NU" -d hesap \
  -v apply="$APPLY" -v ON_ERROR_STOP=1 \
  -f /sql/07-x100-old-contracts.sql
