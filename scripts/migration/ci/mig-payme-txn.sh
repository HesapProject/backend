#!/usr/bin/env bash
# Payme daromad migratsiyasi: payment.transaction (PAID) -> "user".purchases,
# avvalgi user_package legacy purchase'larni o'chirib. [apply] sarlavhada bo'lsa yozadi.
set -uo pipefail
APPLY=false
echo "${CI_COMMIT_TITLE:-}" | grep -q '\[apply\]' && APPLY=true
echo "APPLY=$APPLY"

MIG=/tmp/mig-payme-txn
rm -rf "$MIG"; mkdir -p "$MIG"; chmod 777 "$MIG"

OP="$(docker exec pg_prod printenv POSTGRES_PASSWORD 2>/dev/null || true)"
NU="$(docker exec main-service printenv POSTGRES_USER 2>/dev/null || echo postgres)"
NP="$(docker exec main-service printenv POSTGRES_PASSWORD 2>/dev/null || true)"

echo "=== EXPORT: payment.transaction (PAID) ==="
docker run --rm --network app_net -v "$MIG":/mig -e PGPASSWORD="$OP" postgres:16-alpine \
  psql -h pg_prod -U postgres -d payment -c \
  "\\copy (SELECT id AS txn_id, user_id, user_pnfl, amount, paid_date, created_date FROM transaction WHERE status='PAID') TO '/mig/payme_txn.csv' WITH (FORMAT csv, HEADER true)"
wc -l "$MIG"/payme_txn.csv

echo "=== EXPORT: trust.user_package id'lari (legacy o'chirish uchun) ==="
docker run --rm --network app_net -v "$MIG":/mig -e PGPASSWORD="$OP" postgres:16-alpine \
  psql -h pg_prod -U postgres -d trust -c \
  "\\copy (SELECT id FROM user_package) TO '/mig/upids.csv' WITH (FORMAT csv, HEADER true)"
wc -l "$MIG"/upids.csv

echo "=== IMPORT + TRANSFORM (yangi hesap, apply=$APPLY) ==="
docker run --rm --network hesap-postgres -v "$MIG":/mig -v "$CI_PROJECT_DIR/scripts/migration":/sql -e PGPASSWORD="$NP" postgres:16-alpine \
  psql -h postgres -p 5435 -U "$NU" -d hesap -v ON_ERROR_STOP=1 -v apply="$APPLY" -f /sql/12-migrate-payme-transactions.sql
