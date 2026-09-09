#!/usr/bin/env bash
# Legacy trust DB'dagi shablon (template) jadvallarini aniqlash.
# Ishga tushirish: prod server ichida yoki [introspect-templates] CI gate orqali.
set -uo pipefail

OP="$(docker exec pg_prod printenv POSTGRES_PASSWORD 2>/dev/null || true)"
if [ -z "$OP" ]; then
  echo "ERROR: pg_prod container topilmadi yoki POSTGRES_PASSWORD yo'q"
  exit 1
fi

# SQL'ni tmp fayl orqali docker'ga uzatamiz (heredoc + command substitution bash quirks'idan qochish).
SQL_FILE="$(mktemp)"
cat >"$SQL_FILE" <<'EOF'
\echo === 1) trust DB template-like jadvallar ===
SELECT table_schema || '.' || table_name AS tbl
FROM information_schema.tables
WHERE table_schema = 'public'
  AND (
       table_name ILIKE '%template%'
    OR table_name ILIKE '%jrxml%'
    OR table_name ILIKE '%form%'
    OR table_name ILIKE '%contract_type%'
    OR table_name ILIKE '%prepare%'
  )
ORDER BY table_name;

\echo === 2) topilgan jadvallarning ustunlari ===
SELECT
  c.table_name || ' :: ' || c.column_name || ' :: ' || c.data_type AS col
FROM information_schema.columns c
JOIN information_schema.tables t
  ON t.table_schema = c.table_schema
 AND t.table_name = c.table_name
WHERE c.table_schema = 'public'
  AND (
       t.table_name ILIKE '%template%'
    OR t.table_name ILIKE '%jrxml%'
    OR t.table_name ILIKE '%form%'
    OR t.table_name ILIKE '%contract_type%'
    OR t.table_name ILIKE '%prepare%'
  )
ORDER BY c.table_name, c.ordinal_position;

\echo === 3) til/kontent ustunlari (uz/ru/en/jrxml/content/data/body) ===
SELECT c.table_name || ' :: ' || c.column_name AS lang_col
FROM information_schema.columns c
JOIN information_schema.tables t
  ON t.table_schema = c.table_schema
 AND t.table_name = c.table_name
WHERE c.table_schema = 'public'
  AND (
       t.table_name ILIKE '%template%'
    OR t.table_name ILIKE '%jrxml%'
    OR t.table_name ILIKE '%form%'
    OR t.table_name ILIKE '%contract_type%'
    OR t.table_name ILIKE '%prepare%'
  )
  AND (
       c.column_name ILIKE '%_uz'
    OR c.column_name ILIKE '%_ru'
    OR c.column_name ILIKE '%_en'
    OR c.column_name ILIKE '%jrxml%'
    OR c.column_name ILIKE '%content%'
    OR c.column_name ILIKE '%data%'
    OR c.column_name ILIKE '%body%'
  )
ORDER BY c.table_name, c.column_name;

\echo === 4) qatorlar soni har jadval boyicha ===
DO $$
DECLARE
  r RECORD;
  n BIGINT;
BEGIN
  FOR r IN
    SELECT table_name FROM information_schema.tables
    WHERE table_schema = 'public'
      AND (
           table_name ILIKE '%template%'
        OR table_name ILIKE '%jrxml%'
        OR table_name ILIKE '%form%'
        OR table_name ILIKE '%contract_type%'
        OR table_name ILIKE '%prepare%'
      )
  LOOP
    EXECUTE format('SELECT COUNT(*) FROM public.%I', r.table_name) INTO n;
    RAISE NOTICE '% -> % qator', r.table_name, n;
  END LOOP;
END $$;
EOF

docker run --rm -i --network app_net -e PGPASSWORD="$OP" -v "$SQL_FILE:/query.sql" postgres:16-alpine \
  psql -h pg_prod -U postgres -d trust -v ON_ERROR_STOP=0 -f /query.sql

rm -f "$SQL_FILE"
