#!/usr/bin/env bash
# Migratsiya holati: eski trust DB'dagi barcha jadval row-count'lari va yangi
# hesap DB'dagi migration.* xarita jadvallari + asosiy target jadvallar soni.
# [columns] marker bo'lsa qolgan (ko'chirilmagan) jadvallarning ustunlarini chiqaradi.
# Nima ko'chirilgan, nima qolganini aniqlash uchun — faqat O'QIYDI, yozmaydi.
set -uo pipefail

OP="$(docker exec pg_prod printenv POSTGRES_PASSWORD 2>/dev/null || true)"
NU="$(docker exec main-service printenv POSTGRES_USER 2>/dev/null || echo postgres)"
NP="$(docker exec main-service printenv POSTGRES_PASSWORD 2>/dev/null || true)"

if echo "${CI_COMMIT_TITLE:-}" | grep -q '\[newcols\]'; then
  echo "=================== YANGI hesap DB — target jadval ustunlari ==================="
  docker run --rm -i --network hesap-postgres -e PGPASSWORD="$NP" postgres:16-alpine \
    psql -h postgres -p 5435 -U "$NU" -d hesap -v ON_ERROR_STOP=0 <<'PSQL'
SELECT c.table_schema || '.' || c.table_name || ' :: ' || c.column_name || ' :: ' || c.data_type AS col
FROM information_schema.columns c
WHERE (c.table_schema='integration' AND c.table_name IN ('notification'))
   OR (c.table_schema='document' AND c.table_name IN ('notices','claims','payment_requests','delay_requests','payment_transactions','product_requests','payments'))
   OR (c.table_schema='user' AND c.table_name IN ('user_package_usage','session','user_package'))
ORDER BY c.table_schema, c.table_name, c.ordinal_position;
PSQL
  exit 0
fi

if echo "${CI_COMMIT_TITLE:-}" | grep -q '\[columns\]'; then
  echo "=================== ESKI trust DB — qolgan jadvallar ustunlari ==================="
  docker run --rm -i --network app_net -e PGPASSWORD="$OP" postgres:16-alpine \
    psql -h pg_prod -U postgres -d trust -v ON_ERROR_STOP=0 <<'PSQL'
SELECT c.table_name || ' :: ' || c.column_name || ' :: ' || c.data_type AS col
FROM information_schema.columns c
WHERE c.table_schema='public'
  AND c.table_name IN ('passport','passport_full_data','myid_sdk_results','device',
                       'notification','payment_history','payment_requests',
                       'contract_transaction','contract_notice','contract_reports',
                       'user_package_usage','user_documents','cdn_data',
                       'partner','client_partner','client_partner_users','session')
ORDER BY c.table_name, c.ordinal_position;

\echo '=== namunalar (1 qatordan) ==='
SELECT 'passport' t, left(row(p.*)::text, 300) FROM passport p LIMIT 1;
SELECT 'device' t, left(row(d.*)::text, 300) FROM device d LIMIT 1;
SELECT 'notification' t, left(row(n.*)::text, 300) FROM notification n LIMIT 1;
SELECT 'payment_history' t, left(row(x.*)::text, 300) FROM payment_history x LIMIT 1;
SELECT 'contract_transaction' t, left(row(x.*)::text, 300) FROM contract_transaction x LIMIT 1;
SELECT 'contract_notice' t, left(row(x.*)::text, 300) FROM contract_notice x LIMIT 1;
SELECT 'contract_reports' t, left(row(x.*)::text, 300) FROM contract_reports x LIMIT 1;
SELECT 'user_package_usage' t, left(row(x.*)::text, 300) FROM user_package_usage x LIMIT 1;
SELECT 'user_documents' t, left(row(x.*)::text, 300) FROM user_documents x LIMIT 1;
SELECT 'cdn_data' t, left(row(x.*)::text, 300) FROM cdn_data x LIMIT 1;
PSQL
  exit 0
fi

echo "=================== ESKI trust DB — jadval row count ==================="
docker run --rm -i --network app_net -e PGPASSWORD="$OP" postgres:16-alpine \
  psql -h pg_prod -U postgres -d trust -v ON_ERROR_STOP=0 <<'PSQL'
SELECT relname AS old_table, n_live_tup AS approx_rows
FROM pg_stat_user_tables
WHERE schemaname='public'
ORDER BY n_live_tup DESC;
PSQL

echo "=================== YANGI hesap DB — migration.* xaritalar ==================="
docker run --rm -i --network hesap-postgres -e PGPASSWORD="$NP" postgres:16-alpine \
  psql -h postgres -p 5435 -U "$NU" -d hesap -v ON_ERROR_STOP=0 <<'PSQL'
SELECT schemaname||'.'||relname AS mig_table, n_live_tup AS approx_rows
FROM pg_stat_user_tables
WHERE schemaname='migration'
ORDER BY relname;

\echo '=================== YANGI hesap DB — target jadvallar ==================='
SELECT schemaname||'.'||relname AS new_table, n_live_tup AS approx_rows
FROM pg_stat_user_tables
WHERE schemaname IN ('user','document','billing','integration','main')
ORDER BY schemaname, n_live_tup DESC;
PSQL
