#!/usr/bin/env bash
# Item 2+4 migratsiya: eski trust CSV export → yangi hesap staging+transform.
# [apply] marker bo'lsa yozadi; aks holda faqat PREVIEW.
set -uo pipefail
APPLY=false
echo "${CI_COMMIT_TITLE:-}" | grep -q '\[apply\]' && APPLY=true
echo "APPLY=$APPLY"

MIG=/tmp/mig-perm-pkg
rm -rf "$MIG"; mkdir -p "$MIG"; chmod 777 "$MIG"

OP="$(docker exec pg_prod printenv POSTGRES_PASSWORD 2>/dev/null || true)"
NU="$(docker exec main-service printenv POSTGRES_USER 2>/dev/null || echo postgres)"
NP="$(docker exec main-service printenv POSTGRES_PASSWORD 2>/dev/null || true)"

oldcopy() { # $1=SQL  $2=fayl
  docker run --rm --network app_net -v "$MIG":/mig -e PGPASSWORD="$OP" postgres:16-alpine \
    psql -h pg_prod -U postgres -d trust -c "\\copy ($1) TO '/mig/$2' WITH (FORMAT csv, HEADER true)"
}

echo "=== EXPORT (eski trust) ==="
oldcopy "SELECT id,user_f,user_t,passport,partner,contract,payability,created_at,updated_at,active FROM permission" permission.csv
oldcopy "SELECT id,user_f,user_t,contract,payability,passport,partner,status,created_at,updated_at,is_deleted FROM permission_request" permission_req.csv
oldcopy "SELECT id,name_uz,name_ru,name_en,price,document_count,expired_date,created_date,last_modified_date,deleted,type,document_type_id,description_uz,description_ru,description_en FROM document_package" document_package.csv
oldcopy "SELECT id,user_id,package_id,price_packet,document_count,expired_date,updated_at,created_at,promo_id,promo_code,additional_document_count,transaction_id FROM user_package" user_package.csv
wc -l "$MIG"/*.csv

echo "=== IMPORT + TRANSFORM (yangi hesap, apply=$APPLY) ==="
docker run --rm --network hesap-postgres -v "$MIG":/mig -v "$CI_PROJECT_DIR/scripts/migration":/sql -e PGPASSWORD="$NP" postgres:16-alpine \
  psql -h postgres -p 5435 -U "$NU" -d hesap -v apply="$APPLY" -f /sql/03-perm-pkg.sql
