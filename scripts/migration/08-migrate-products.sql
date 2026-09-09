-- Eski shartnomalar mahsulotlari: trust.contract.products (JSON matn,
-- [{"name","unit","count","price","description"}, ...]) -> document.products.
-- Narx eski so'mda -> tiyinga *100 (contracts.price bilan bir xil konvensiya).
-- unit erkin matn (108+ xil yozilish) -> DONA/KG/LITR ga normalizatsiya qilinadi
-- (aniq kg/litr signali bo'lmasa DONA — ustunlarning katta ko'pchiligi shunga mos).
-- Idempotent: deterministik uuid_v5(contract_id, massiv indeksi) + ON CONFLICT DO NOTHING.
-- :apply=false -> PREVIEW.
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE SCHEMA IF NOT EXISTS migration;

DROP TABLE IF EXISTS migration.stage_products;
CREATE TABLE migration.stage_products (id bigint, products text);
\copy migration.stage_products FROM '/mig/products.csv' WITH (FORMAT csv, HEADER true)

\echo '=== STAGE: qancha shartnomada mahsulot JSON bor ==='
SELECT count(*) FROM migration.stage_products;

\echo '=== shundan nechtasi ko-chirilgan shartnomaga bogliq (contract_id_map) ==='
SELECT count(*) FROM migration.stage_products s
JOIN migration.contract_id_map cm ON cm.old_id = s.id;

-- Har bir mahsulot qatori — massiv elementi + indeks (deterministik id uchun).
DROP TABLE IF EXISTS migration.stage_product_items;
CREATE TABLE migration.stage_product_items AS
SELECT s.id AS old_contract_id, cm.new_id AS document_id, (t.idx - 1) AS item_index, t.elem
FROM migration.stage_products s
JOIN migration.contract_id_map cm ON cm.old_id = s.id
CROSS JOIN LATERAL jsonb_array_elements(s.products::jsonb) WITH ORDINALITY AS t(elem, idx);

\echo '=== jami mahsulot qatorlari (yoziladigan) ==='
SELECT count(*) FROM migration.stage_product_items;

\echo '=== unit normalizatsiyasi taqsimoti (nazorat uchun) ==='
SELECT
  CASE
    WHEN lower(trim(coalesce(elem->>'unit',''))) ~ 'kg|kilo|кг|gram|gramm|грамм'
      THEN 'KG'
    WHEN lower(trim(coalesce(elem->>'unit',''))) ~ '^(litr|liter|l|л|millilitr)$'
      THEN 'LITR'
    ELSE 'DONA'
  END AS mapped_unit,
  count(*)
FROM migration.stage_product_items
GROUP BY 1 ORDER BY 2 DESC;

\echo '=== narx/soni bosh (default qiymat qollaniladigan) qatorlar ==='
SELECT count(*) FILTER (WHERE nullif(elem->>'price','') IS NULL) AS price_null,
       count(*) FILTER (WHERE nullif(elem->>'count','') IS NULL) AS count_null
FROM migration.stage_product_items;

\if :apply

\echo '>>> APPLY: document.products'
INSERT INTO document.products
  (id, document_id, name, unit, price, quantity, amount, delivery_at, field_values,
   deleted, created_date, last_modified_date)
SELECT
  uuid_generate_v5(uuid_ns_url(), 'hesap:old-product:' || old_contract_id || ':' || item_index),
  document_id,
  nullif(trim(elem->>'name'), ''),
  CASE
    WHEN lower(trim(coalesce(elem->>'unit',''))) ~ 'kg|kilo|кг|gram|gramm|грамм'
      THEN 'KG'
    WHEN lower(trim(coalesce(elem->>'unit',''))) ~ '^(litr|liter|l|л|millilitr)$'
      THEN 'LITR'
    ELSE 'DONA'
  END,
  coalesce(nullif(elem->>'price','')::numeric, 0) * 100,
  coalesce(nullif(elem->>'count','')::numeric, 1),
  coalesce(nullif(elem->>'price','')::numeric, 0) * 100
    * coalesce(nullif(elem->>'count','')::numeric, 1),
  NULL,
  NULL,
  FALSE,
  coalesce(c.created_date, now()),
  coalesce(c.created_date, now())
FROM migration.stage_product_items spi
JOIN document.contracts c ON c.id = spi.document_id
ON CONFLICT (id) DO NOTHING;

\echo '=== YAKUN: document.products jami ==='
SELECT count(*) FROM document.products;

\else
\echo 'PREVIEW rejimi — yozilmadi. Yozish uchun commit xabariga [apply] qo-shing.'
\endif
