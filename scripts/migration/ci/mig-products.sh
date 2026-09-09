#!/usr/bin/env bash
# Eski shartnomalar mahsulotlari migratsiyasi: eski trust.contract.products (JSON matn)
# -> yangi document.products. [apply] marker bo'lsa yozadi, aks holda PREVIEW.
set -uo pipefail
APPLY=false
echo "${CI_COMMIT_TITLE:-}" | grep -q '\[apply\]' && APPLY=true
echo "APPLY=$APPLY"

MIG=/tmp/mig-products
rm -rf "$MIG"; mkdir -p "$MIG"; chmod 777 "$MIG"

OP="$(docker exec pg_prod printenv POSTGRES_PASSWORD 2>/dev/null || true)"
NU="$(docker exec main-service printenv POSTGRES_USER 2>/dev/null || echo postgres)"
NP="$(docker exec main-service printenv POSTGRES_PASSWORD 2>/dev/null || true)"

echo "=== EXPORT (eski trust) ==="
docker run --rm --network app_net -v "$MIG":/mig -e PGPASSWORD="$OP" postgres:16-alpine \
  psql -h pg_prod -U postgres -d trust -c \
  "\\copy (SELECT id, products FROM contract WHERE products IS NOT NULL AND products <> '' AND products <> '[]' AND products::text ~ '^\\s*\\[') TO '/mig/products.csv' WITH (FORMAT csv, HEADER true)"
wc -l "$MIG"/products.csv

echo "=== IMPORT + TRANSFORM (yangi hesap, apply=$APPLY) ==="
docker run --rm --network hesap-postgres -v "$MIG":/mig -v "$CI_PROJECT_DIR/scripts/migration":/sql -e PGPASSWORD="$NP" postgres:16-alpine \
  psql -h postgres -p 5435 -U "$NU" -d hesap -v ON_ERROR_STOP=1 -v apply="$APPLY" -f /sql/08-migrate-products.sql
