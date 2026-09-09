#!/usr/bin/env bash
set -uo pipefail
NU="$(docker exec main-service printenv POSTGRES_USER 2>/dev/null || echo postgres)"
NP="$(docker exec main-service printenv POSTGRES_PASSWORD 2>/dev/null || true)"
docker run --rm -i --network hesap-postgres -e PGPASSWORD="$NP" postgres:16-alpine \
  psql -h postgres -p 5435 -U "$NU" -d hesap -v ON_ERROR_STOP=1 <<'PSQL'
BEGIN;
\echo '=== OLDIN: exp_date IS NULL (deleted=false) ==='
SELECT count(*) AS null_rows, count(DISTINCT user_in) AS users FROM "user".user_package WHERE exp_date IS NULL AND deleted=false;
\echo '=== UPDATE: exp_date = now() + 90 kun ==='
UPDATE "user".user_package
   SET exp_date = NOW() + INTERVAL '90 days', updated_at = NOW()
 WHERE exp_date IS NULL AND deleted = false;
\echo '=== KEYIN: qolgan NULL (0 bo`lishi kerak) ==='
SELECT count(*) AS still_null FROM "user".user_package WHERE exp_date IS NULL AND deleted=false;
\echo '=== Test mijoz (30401942600209) paketlari endi ==='
SELECT id, exp_date::date, deleted FROM "user".user_package WHERE user_in='30401942600209';
COMMIT;
PSQL
