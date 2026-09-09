#!/usr/bin/env bash
# Eski paket xaridlari migratsiyasi: trust.user_package -> "user".purchases.
# [apply] marker (commit SARLAVHASIDA) bo'lsa yozadi, aks holda PREVIEW.
set -uo pipefail
APPLY=false
echo "${CI_COMMIT_TITLE:-}" | grep -q '\[apply\]' && APPLY=true
echo "APPLY=$APPLY"

MIG=/tmp/mig-legacy-purchases
rm -rf "$MIG"; mkdir -p "$MIG"; chmod 777 "$MIG"

OP="$(docker exec pg_prod printenv POSTGRES_PASSWORD 2>/dev/null || true)"
NU="$(docker exec main-service printenv POSTGRES_USER 2>/dev/null || echo postgres)"
NP="$(docker exec main-service printenv POSTGRES_PASSWORD 2>/dev/null || true)"

echo "=== EXPORT (eski trust) ==="
docker run --rm --network app_net -v "$MIG":/mig -e PGPASSWORD="$OP" postgres:16-alpine \
  psql -h pg_prod -U postgres -d trust -c \
  "\\copy (SELECT up.id, up.user_id, u.pnfl AS user_pnfl, dp.name_uz AS pkg_name, up.price_packet, up.promo_code, up.created_at FROM user_package up LEFT JOIN users u ON u.id = up.user_id LEFT JOIN document_package dp ON dp.id = up.package_id) TO '/mig/legacy_purchases.csv' WITH (FORMAT csv, HEADER true)"
wc -l "$MIG"/legacy_purchases.csv

echo "=== IMPORT + TRANSFORM (yangi hesap, apply=$APPLY) ==="
docker run --rm --network hesap-postgres -v "$MIG":/mig -v "$CI_PROJECT_DIR/scripts/migration":/sql -e PGPASSWORD="$NP" postgres:16-alpine \
  psql -h postgres -p 5435 -U "$NU" -d hesap -v ON_ERROR_STOP=1 -v apply="$APPLY" -f /sql/11-migrate-legacy-purchases.sql
