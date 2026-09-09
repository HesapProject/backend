-- Eski paket xaridlari: trust.user_package (594 ta) -> "user".purchases.
-- Eski DB narxni tiyinda saqlaydi (price_packet=15000000 => 150000 so'm) -> /100.
-- Xaridor PINFL: trust.users.pnfl (eksportda qo'shilgan). Paket: nom bo'yicha
-- yangi "user".packages.id ga map. To'lov usuli: hammasi tashqi (transaction_id
-- bor) -> 'PAYME' (foydalanuvchi qarori). FAQAT xarid tarixi yoziladi — yangi
-- user_package grant QILINMAYDI (eski/muddati o'tgan paketlar).
-- Idempotent: deterministik uuid_v5(old_id) + ON CONFLICT (id) DO NOTHING.
-- :apply=false -> PREVIEW.
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE SCHEMA IF NOT EXISTS migration;

DROP TABLE IF EXISTS migration.stage_legacy_purchases;
CREATE TABLE migration.stage_legacy_purchases (
  old_id bigint, user_id bigint, user_pnfl text, pkg_name text,
  price_packet bigint, promo_code text, created_at timestamp);
\copy migration.stage_legacy_purchases FROM '/mig/legacy_purchases.csv' WITH (FORMAT csv, HEADER true)

\echo '=== STAGE: jami eski xaridlar ==='
SELECT count(*) FROM migration.stage_legacy_purchases;

\echo '=== PINFL bor/yo''q ==='
SELECT count(*) FILTER (WHERE nullif(user_pnfl,'') IS NOT NULL) AS pnfl_bor,
       count(*) FILTER (WHERE nullif(user_pnfl,'') IS NULL) AS pnfl_yoq
FROM migration.stage_legacy_purchases;

\echo '=== paket nomi yangi paketga mos keladimi ==='
SELECT count(*) FILTER (WHERE pk.id IS NOT NULL) AS mos,
       count(*) FILTER (WHERE pk.id IS NULL) AS mos_emas
FROM migration.stage_legacy_purchases s
LEFT JOIN LATERAL (
  SELECT id FROM "user".packages p
  WHERE p.name_uz = s.pkg_name
  ORDER BY p.deleted ASC, p.created_date LIMIT 1) pk ON true;

\echo '=== jami tarixiy daromad (so''m) ==='
SELECT to_char(COALESCE(SUM(price_packet),0)/100.0, 'FM999 999 999 990') AS jami_som
FROM migration.stage_legacy_purchases;

\echo '=== paket kesimida (mapped=false — yangi paketga mos kelmadi) ==='
SELECT pkg_name, cnt, to_char(som, 'FM999 999 990') AS som, mapped
FROM (
  SELECT s.pkg_name AS pkg_name, count(*) AS cnt,
         SUM(s.price_packet) / 100.0 AS som, (pk.id IS NOT NULL) AS mapped
  FROM migration.stage_legacy_purchases s
  LEFT JOIN LATERAL (
    SELECT id FROM "user".packages p
    WHERE p.name_uz = s.pkg_name
    ORDER BY p.deleted ASC, p.created_date LIMIT 1) pk ON true
  GROUP BY s.pkg_name, (pk.id IS NOT NULL)
) x
ORDER BY cnt DESC;

\if :apply

\echo '>>> APPLY: "user".purchases (eski xaridlar)'
INSERT INTO "user".purchases
  (id, amount, user_in, promo, payment_method, unit_type, unit_id,
   created_by, updated_by, created_at, updated_at)
SELECT
  uuid_generate_v5(uuid_ns_url(), 'hesap:legacy-purchase:' || s.old_id),
  s.price_packet / 100.0,
  nullif(s.user_pnfl, ''),
  nullif(s.promo_code, ''),
  'PAYME',
  'PACKAGE',
  pk.id,
  um.new_id,
  um.new_id,
  s.created_at,
  s.created_at
FROM migration.stage_legacy_purchases s
LEFT JOIN LATERAL (
  SELECT id FROM "user".packages p
  WHERE p.name_uz = s.pkg_name
  ORDER BY p.deleted ASC, p.created_date LIMIT 1) pk ON true
LEFT JOIN migration.user_id_map um ON um.old_id = s.user_id
ON CONFLICT (id) DO NOTHING;

\echo '=== YAKUN: purchases jami / bugungi Tushum mantigi (butun tarix) ==='
SELECT count(*) AS purchases_jami FROM "user".purchases;
SELECT to_char(COALESCE(SUM(amount),0),'FM999 999 999 990') AS tashqi_daromad_som
FROM "user".purchases WHERE payment_method NOT IN ('BALANCE','CONTROL');

\else
\echo 'PREVIEW rejimi — yozilmadi. Yozish uchun commit sarlavhasiga [apply] qo-shing.'
\endif
