#!/usr/bin/env bash
# Legacy trust.contract_type → document.contract_template migratsiya.
# name_uz/ru/en, lender_uz/ru/en → seller_name_*, debtor_uz/ru/en → buyer_name_*,
# example_uz/ru/en → template_data_uz/ru/en, min_witness → witness_count,
# active → status (PUBLISHED/CREATED), is_deleted → deleted.
# Idempotent: uuid_generate_v5(url_ns, 'hesap:legacy-contract-type:<id>') asosida ON CONFLICT DO NOTHING.
# Ishga tushirish: [migrate-templates] CI gate orqali.
set -uo pipefail

OP="$(docker exec pg_prod printenv POSTGRES_PASSWORD 2>/dev/null || true)"
NU="$(docker exec main-service printenv POSTGRES_USER 2>/dev/null || echo postgres)"
NP="$(docker exec main-service printenv POSTGRES_PASSWORD 2>/dev/null || true)"
if [ -z "$OP" ] || [ -z "$NP" ]; then
  echo "ERROR: pg_prod yoki main-service container topilmadi"
  exit 1
fi

MIG=/tmp/mig-templates; rm -rf "$MIG"; mkdir -p "$MIG"; chmod 777 "$MIG"

# 1) Legacy'dan contract_type export — COPY ... TO STDOUT + shell redirect (\copy meta-command
# ko'p qatorli SQL faylda ishlamaydi, faqat bir qatorda)
echo "=== 1) Legacy'dan contract_type export ==="
docker run --rm -i --network app_net -e PGPASSWORD="$OP" postgres:16-alpine \
  psql -h pg_prod -U postgres -d trust -Atc "COPY (SELECT id::text, coalesce(name_uz,''), coalesce(name_ru,''), coalesce(name_en,''), coalesce(lender_uz,''), coalesce(lender_ru,''), coalesce(lender_en,''), coalesce(debtor_uz,''), coalesce(debtor_ru,''), coalesce(debtor_en,''), coalesce(example_uz,''), coalesce(example_ru,''), coalesce(example_en,''), coalesce(min_witness::text,'0'), coalesce(price::text,'0'), coalesce(product::text,'false'), coalesce(initial_payment::text,'false'), coalesce(active::text,'true'), coalesce(is_deleted::text,'false'), coalesce(created_at::text,''), coalesce(updated_at::text,'') FROM public.contract_type) TO STDOUT WITH (FORMAT csv, FORCE_QUOTE *);" > "$MIG/ct.csv"

# Header'ni qo'lda yozamiz (STDOUT dan chiqmaydi HEADER'siz)
HEADER="id,name_uz,name_ru,name_en,lender_uz,lender_ru,lender_en,debtor_uz,debtor_ru,debtor_en,example_uz,example_ru,example_en,min_witness,price,product,initial_payment,active,is_deleted,created_at,updated_at"
{ echo "$HEADER"; cat "$MIG/ct.csv"; } > "$MIG/ct-with-header.csv"
mv "$MIG/ct-with-header.csv" "$MIG/ct.csv"

echo "Export tugadi. Qatorlar: $(wc -l < "$MIG/ct.csv")"

# 2) Yangi DB'ga import
echo ""
echo "=== 2) Yangi document.contract_template'ga import ==="
SQL_FILE="$(mktemp)"
cat >"$SQL_FILE" <<'EOF'
-- Stage jadval (idempotent — har run'da qayta ochiladi)
CREATE SCHEMA IF NOT EXISTS migration;
DROP TABLE IF EXISTS migration.stage_contract_type;
CREATE TABLE migration.stage_contract_type (
  id            TEXT,
  name_uz       TEXT, name_ru TEXT, name_en TEXT,
  lender_uz     TEXT, lender_ru TEXT, lender_en TEXT,
  debtor_uz     TEXT, debtor_ru TEXT, debtor_en TEXT,
  example_uz    TEXT, example_ru TEXT, example_en TEXT,
  min_witness   TEXT,
  price         TEXT,
  product       TEXT,
  initial_payment TEXT,
  active        TEXT,
  is_deleted    TEXT,
  created_at    TEXT,
  updated_at    TEXT
);
\copy migration.stage_contract_type FROM '/mig/ct.csv' WITH (FORMAT csv, HEADER true);

\echo === stage'da nechta qator ===
SELECT COUNT(*) FROM migration.stage_contract_type;

-- ID xaritasi (deterministik UUID)
CREATE TABLE IF NOT EXISTS migration.contract_template_id_map (
  old_id TEXT PRIMARY KEY,
  new_id UUID NOT NULL
);
INSERT INTO migration.contract_template_id_map (old_id, new_id)
SELECT id, uuid_generate_v5(uuid_ns_url(), 'hesap:legacy-contract-type:' || id)
FROM migration.stage_contract_type
ON CONFLICT (old_id) DO NOTHING;

-- Asosiy insert: document.contract_template'ga (mavjud bo'lmagan yozuvlar)
INSERT INTO document.contract_template (
  id, name_uz, name_ru, name_en,
  seller_name_uz, seller_name_ru, seller_name_en,
  buyer_name_uz, buyer_name_ru, buyer_name_en,
  template_data_uz, template_data_ru, template_data_en,
  witness_count, amount,
  product_enabled, initial_payment_enabled, payment_schedule_enabled, witness_enabled,
  status, template_type,
  individual_verification_type, legal_verification_type,
  deleted, created_date, last_modified_date
)
SELECT
  m.new_id,
  NULLIF(s.name_uz,''), NULLIF(s.name_ru,''), NULLIF(s.name_en,''),
  NULLIF(s.lender_uz,''), NULLIF(s.lender_ru,''), NULLIF(s.lender_en,''),
  NULLIF(s.debtor_uz,''), NULLIF(s.debtor_ru,''), NULLIF(s.debtor_en,''),
  NULLIF(s.example_uz,''), NULLIF(s.example_ru,''), NULLIF(s.example_en,''),
  NULLIF(s.min_witness,'')::int,
  NULLIF(s.price,'')::float8,
  s.product = 'true' OR s.product = 't',
  s.initial_payment = 'true' OR s.initial_payment = 't',
  TRUE,
  TRUE,
  CASE WHEN s.active = 'true' OR s.active = 't' THEN 'PUBLISHED' ELSE 'CREATED' END,
  'C2C',
  'OTP_SMS',
  'E_IMZO',
  COALESCE(NULLIF(s.is_deleted,'')::bool, FALSE),
  COALESCE(NULLIF(s.created_at,'')::timestamp, NOW()),
  COALESCE(NULLIF(s.updated_at,'')::timestamp, NOW())
FROM migration.stage_contract_type s
JOIN migration.contract_template_id_map m ON m.old_id = s.id
ON CONFLICT (id) DO UPDATE SET
  -- 3 til data — mavjud bo'lmagan tillarga ma'lumot to'ldiramiz (Uz saqlanadi)
  template_data_ru = COALESCE(document.contract_template.template_data_ru, EXCLUDED.template_data_ru),
  template_data_en = COALESCE(document.contract_template.template_data_en, EXCLUDED.template_data_en),
  name_ru = COALESCE(NULLIF(document.contract_template.name_ru,''), EXCLUDED.name_ru),
  name_en = COALESCE(NULLIF(document.contract_template.name_en,''), EXCLUDED.name_en),
  seller_name_ru = COALESCE(NULLIF(document.contract_template.seller_name_ru,''), EXCLUDED.seller_name_ru),
  seller_name_en = COALESCE(NULLIF(document.contract_template.seller_name_en,''), EXCLUDED.seller_name_en),
  buyer_name_ru = COALESCE(NULLIF(document.contract_template.buyer_name_ru,''), EXCLUDED.buyer_name_ru),
  buyer_name_en = COALESCE(NULLIF(document.contract_template.buyer_name_en,''), EXCLUDED.buyer_name_en);

\echo === Import natijasi ===
SELECT COUNT(*) AS total_contract_templates FROM document.contract_template WHERE deleted = FALSE;
SELECT
  COUNT(*) FILTER (WHERE template_data_uz IS NOT NULL) AS with_uz,
  COUNT(*) FILTER (WHERE template_data_ru IS NOT NULL) AS with_ru,
  COUNT(*) FILTER (WHERE template_data_en IS NOT NULL) AS with_en
FROM document.contract_template WHERE deleted = FALSE;
EOF

docker run --rm -i --network hesap-postgres -v "$MIG":/mig -v "$SQL_FILE:/query.sql" -e PGPASSWORD="$NP" postgres:16-alpine \
  psql -h postgres -p 5435 -U "$NU" -d hesap -v ON_ERROR_STOP=0 -f /query.sql

rm -f "$SQL_FILE"
echo ""
echo "=== TUGADI ==="
