#!/usr/bin/env bash
# Qolgan legacy ma'lumotlar migratsiyasi: eski trust CSV export -> yangi hesap
# staging+transform (07-remaining.sql). [apply] marker bo'lsa yozadi; aks holda PREVIEW.
set -uo pipefail
APPLY=false
echo "${CI_COMMIT_TITLE:-}" | grep -q '\[apply\]' && APPLY=true
echo "APPLY=$APPLY"

MIG=/tmp/mig-remaining
rm -rf "$MIG"; mkdir -p "$MIG"; chmod 777 "$MIG"

OP="$(docker exec pg_prod printenv POSTGRES_PASSWORD 2>/dev/null || true)"
NU="$(docker exec main-service printenv POSTGRES_USER 2>/dev/null || echo postgres)"
NP="$(docker exec main-service printenv POSTGRES_PASSWORD 2>/dev/null || true)"

oldcopy() { # $1=SQL  $2=fayl
  docker run --rm --network app_net -v "$MIG":/mig -e PGPASSWORD="$OP" postgres:16-alpine \
    psql -h pg_prod -U postgres -d trust -c "\\copy ($1) TO '/mig/$2' WITH (FORMAT csv, HEADER true)"
}

echo "=== EXPORT (eski trust) ==="
oldcopy "SELECT id,user_id,pnfl,number,first_name,middle_name,last_name,birth_date,birth_place,give_place,start_date,end_date,is_man,status,active,is_deleted,created_at,updated_at FROM passport" passport.csv
oldcopy "SELECT id,data_id,type,user_id,title_uz,title_ru,title_en,body_uz,body_ru,body_en,image,created_at,is_deleted FROM notification WHERE user_id IS NOT NULL" notification.csv
oldcopy "SELECT id,contract_id,number,lender_id,debtor_id,status,name_uz,name_ru,name_en,created_at,updated_at FROM contract_notice" notice.csv
oldcopy "SELECT id,contract_id,lender_id,debtor_id,status,created_at,updated_at FROM contract_reports" report.csv
oldcopy "SELECT id,payment_id,amount,status,created_at,updated_at,accepted_at,image,comment,lender_id,debtor_id,date_change_from,date_change_to,is_payment,is_date_change,contract_id,is_active FROM payment_requests" payreq.csv
oldcopy "SELECT id,user_id,contract_id,type_id,created_at FROM user_package_usage" upu.csv
wc -l "$MIG"/*.csv

echo "=== IMPORT + TRANSFORM (yangi hesap, apply=$APPLY) ==="
docker run --rm --network hesap-postgres -v "$MIG":/mig -v "$CI_PROJECT_DIR/scripts/migration":/sql -e PGPASSWORD="$NP" postgres:16-alpine \
  psql -h postgres -p 5435 -U "$NU" -d hesap -v ON_ERROR_STOP=1 -v apply="$APPLY" -f /sql/07-remaining.sql
