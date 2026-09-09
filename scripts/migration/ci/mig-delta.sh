#!/usr/bin/env bash
# DELTA sinxron: eski trust DB'da migratsiyadan KEYIN qo'shilgan yozuvlarni
# yangi hesap DB'ga ko'chirish. Barcha bosqichlar idempotent (deterministik
# uuid + ON CONFLICT) — mavjudlar o'tkazib yuboriladi, faqat yangilari kiradi.
# Tartib: users(02) -> perm/pkg(03) -> contracts(04) -> payments/witness(05)
#         -> qolganlar(07, mig-remaining.sh orqali).
# [apply] marker bo'lsa yozadi; aks holda faqat PREVIEW.
set -uo pipefail
APPLY=false
echo "${CI_COMMIT_TITLE:-}" | grep -q '\[apply\]' && APPLY=true
echo "APPLY=$APPLY"

MIG=/tmp/mig-delta
rm -rf "$MIG"; mkdir -p "$MIG"; chmod 777 "$MIG"

OP="$(docker exec pg_prod printenv POSTGRES_PASSWORD 2>/dev/null || true)"
NU="$(docker exec main-service printenv POSTGRES_USER 2>/dev/null || echo postgres)"
NP="$(docker exec main-service printenv POSTGRES_PASSWORD 2>/dev/null || true)"

oldpsql() {
  docker run --rm -i --network app_net -v "$MIG":/mig -e PGPASSWORD="$OP" postgres:16-alpine \
    psql -h pg_prod -U postgres -d trust "$@"
}
newpsql() {
  docker run --rm -i --network hesap-postgres -v "$MIG":/mig -v "$CI_PROJECT_DIR/scripts/migration":/sql \
    -w /mig -e PGPASSWORD="$NP" postgres:16-alpine \
    psql -h postgres -p 5435 -U "$NU" -d hesap "$@"
}

# Eski jadvaldan moslashuvchan export: yo'q ustun NULL bo'lib chiqadi.
# $1=jadval $2=fayl $3="col1,col2:asname,..." (":asname" — CSV'dagi nom)
adaptive_export() {
  local table="$1" file="$2" want="$3"
  local sel
  sel=$(oldpsql -tA <<SQL
WITH want(spec, ord) AS (
  SELECT unnest(string_to_array('$want', ',')), generate_subscripts(string_to_array('$want', ','), 1)
)
SELECT string_agg(
  CASE WHEN c.column_name IS NOT NULL THEN quote_ident(split_part(w.spec,':',1))
       ELSE 'NULL' END
  || ' AS ' || coalesce(nullif(split_part(w.spec,':',2),''), split_part(w.spec,':',1)),
  ', ' ORDER BY w.ord)
FROM want w
LEFT JOIN information_schema.columns c
  ON c.table_schema='public' AND c.table_name='$table' AND c.column_name=split_part(w.spec,':',1);
SQL
)
  oldpsql -c "\\copy (SELECT $sel FROM \"$table\") TO '/mig/$file' WITH (FORMAT csv, HEADER true)"
}

echo "=== EXPORT (eski trust) ==="
adaptive_export users old_users.csv "id,username,firstname,lastname,middlename,is_man,bio,birthday,phone,image,pnfl,passport_number,type,tin,inn,address,verified,verified_at,is_deleted,created_at,test_user"
adaptive_export socials old_socials.csv "user_id,phone2,telegram,instagram,facebook"
adaptive_export permission permission.csv "id,user_f,user_t,passport,partner,contract,payability,created_at,updated_at,active"
adaptive_export permission_request permission_req.csv "id,user_f,user_t,contract,payability,passport,partner,status,created_at,updated_at,is_deleted"
adaptive_export document_package document_package.csv "id,name_uz,name_ru,name_en,price,document_count,expired_date,created_date,last_modified_date,deleted,type,document_type_id,description_uz,description_ru,description_en"
adaptive_export user_package user_package.csv "id,user_id,package_id,price_packet,document_count,expired_date,updated_at,created_at,promo_id,promo_code,additional_document_count,transaction_id"
adaptive_export contract contract.csv "id,type_id,status,debtor_pnfl,lender_pnfl,creator_id,amount,currency_id,is_deleted:del,created_at"
adaptive_export payment payment.csv "id,contract_id,lender_pnfl,debtor_pnfl,price,paid,is_paid,status,currency_id,arrangement_at,paid_at,created_at"
adaptive_export payment payment2.csv "id,contract_id,price,arrangement_at,paid_at,is_paid,status,paid,currency_id,is_deleted"
adaptive_export witness witness.csv "id,contract_id,user_id,status,created_at"
wc -l "$MIG"/*.csv

echo "=== 1) USERS (02-import, idempotent) ==="
if [ "$APPLY" = true ]; then
  newpsql -v ON_ERROR_STOP=1 -f /sql/02-import-old-users.sql
else
  newpsql -v ON_ERROR_STOP=0 <<'PSQL'
DROP TABLE IF EXISTS migration.delta_users_preview;
CREATE TABLE migration.delta_users_preview (LIKE migration.old_users);
\copy migration.delta_users_preview FROM '/mig/old_users.csv' WITH (FORMAT csv, HEADER true)
SELECT 'yangi (map yo-q) userlar' t, count(*) FROM migration.delta_users_preview d
WHERE NOT EXISTS (SELECT 1 FROM migration.user_id_map m WHERE m.old_id=d.id);
DROP TABLE migration.delta_users_preview;
PSQL
fi

echo "=== 2) PERMISSION/PACKAGES (03, apply=$APPLY) ==="
newpsql -v ON_ERROR_STOP=0 -v apply="$APPLY" -f /sql/03-perm-pkg.sql

echo "=== 3) CONTRACTS (04, apply=$APPLY) ==="
newpsql -v ON_ERROR_STOP=1 -v apply="$APPLY" <<'PSQL'
CREATE SCHEMA IF NOT EXISTS migration;
CREATE TABLE IF NOT EXISTS migration.stage_c3u (
  id bigint, type_id text, status text, debtor_pnfl text, lender_pnfl text,
  creator_id text, amount text, currency_id text, del text, created_at text);
TRUNCATE migration.stage_c3u;
\copy migration.stage_c3u FROM '/mig/contract.csv' WITH (FORMAT csv, HEADER true)
-- CSV boolean t/f -> true/false (04 filtri 'false' matnini kutadi)
UPDATE migration.stage_c3u SET del = CASE del WHEN 't' THEN 'true' WHEN 'f' THEN 'false' ELSE del END;
SELECT 'stage_c3u' t, count(*) FROM migration.stage_c3u;
SELECT 'stage_c3u yangi (map yo-q, filtrga mos)' t, count(*) FROM migration.stage_c3u c
WHERE NOT EXISTS(SELECT 1 FROM migration.contract_id_map m WHERE m.old_id=c.id)
  AND coalesce(c.lender_pnfl,'')<>'' AND coalesce(c.debtor_pnfl,'')<>''
  AND c.type_id IN ('1','2','3') AND coalesce(c.del,'false')='false';
PSQL
newpsql -v ON_ERROR_STOP=1 -v apply="$APPLY" -f /sql/04-contract-recover.sql

echo "=== 4) PAYMENTS/WITNESSES (05, apply=$APPLY) ==="
newpsql -v ON_ERROR_STOP=1 -v apply="$APPLY" -f /sql/05-payment-witness.sql

echo "=== 4.5) legacy_payment yangilash (pay_id_map va 09 uchun) ==="
newpsql -v ON_ERROR_STOP=1 <<'PSQL'
CREATE TABLE IF NOT EXISTS migration.legacy_payment (id bigint, contract_id bigint,
  price text, arrangement_at text, paid_at text, is_paid text, status text,
  paid text, currency_id text, is_deleted text);
TRUNCATE migration.legacy_payment;
\copy migration.legacy_payment FROM '/mig/payment2.csv' WITH (FORMAT csv, HEADER true)
SELECT 'legacy_payment (yangilangan)' t, count(*) FROM migration.legacy_payment;
PSQL

echo "=== 5) QOLGANLAR (07-remaining, apply=$APPLY) ==="
bash "$CI_PROJECT_DIR/scripts/migration/ci/mig-remaining.sh"

echo "=== YAKUN: asosiy countlar ==="
newpsql -v ON_ERROR_STOP=0 <<'PSQL'
SELECT 'user.user' t, count(*) FROM "user"."user"
UNION ALL SELECT 'migration.user_id_map', count(*) FROM migration.user_id_map
UNION ALL SELECT 'document.contracts', count(*) FROM document.contracts
UNION ALL SELECT 'migration.contract_id_map', count(*) FROM migration.contract_id_map
UNION ALL SELECT 'document.payments', count(*) FROM document.payments
UNION ALL SELECT 'document.witnesses', count(*) FROM document.witnesses
UNION ALL SELECT 'user.white_list', count(*) FROM "user".white_list
UNION ALL SELECT 'user.white_list_request', count(*) FROM "user".white_list_request
UNION ALL SELECT 'user.user_package', count(*) FROM "user".user_package;
PSQL

echo "=== 6) O'ZGARISHLAR sinxroni (09, apply=$APPLY) ==="
newpsql -v ON_ERROR_STOP=1 -v apply="$APPLY" -f /sql/09-delta-updates.sql
