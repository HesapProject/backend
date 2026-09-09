-- Payme/Click orqali PAID bo'lgan, lekin grant/purchase yozilmagan orderlarni tiklash.
-- Sabab: settleIfPending() "BALANCE" method bilan grantPackage chaqirardi ->
-- main withdrawBalance (ichki balans 0, pul Payme'da tashqaridan kelgan) -> xato ->
-- na user_package, na purchase yozilardi (kod fix qilindi: PackageOrderService).
-- Bu skript o'sha 3 ta stuck orderni (PAID, grant/purchase yo'q) qo'lda tiklaydi.
-- Idempotent: deterministik uuid_v5(order.id) + ON CONFLICT (id) DO NOTHING.
-- :apply=false -> PREVIEW (ustun turlari + tanlangan orderlar, yozmaydi).
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

\echo '=== ustun turlari (enum/text — cast kerakligini tekshirish) ==='
SELECT table_name, column_name, data_type, udt_name
FROM information_schema.columns
WHERE table_schema = 'user' AND table_name IN ('purchases', 'user_package')
ORDER BY table_name, ordinal_position;

\echo '=== tiklanadigan orderlar (PAID, shu vaqt oynasida purchase yo''q) ==='
SELECT o.id AS order_id, o.user_in, o.amount, o.provider, o.package_id,
       o.promo_code, o.unique_id, o.created_date, pkg.duration AS pkg_days
FROM integration.payment_order o
JOIN "user".packages pkg ON pkg.id = o.package_id
WHERE o.status = 'PAID'
  AND NOT EXISTS (
    SELECT 1 FROM "user".purchases p
    WHERE p.user_in = o.user_in AND p.unit_id = o.package_id
      AND p.created_at >= o.created_date - interval '10 min'
      AND p.created_at <= o.created_date + interval '30 min')
ORDER BY o.created_date;

\if :apply

\echo '>>> APPLY: "user".user_package (grant)'
INSERT INTO "user".user_package
  (id, package_id, user_in, exp_date, deleted, created_at, updated_at)
SELECT
  uuid_generate_v5(uuid_ns_url(), 'hesap:backfill-userpkg:' || o.id),
  o.package_id,
  o.user_in,
  now() + (pkg.duration || ' days')::interval,
  FALSE,
  now(),
  now()
FROM integration.payment_order o
JOIN "user".packages pkg ON pkg.id = o.package_id
WHERE o.status = 'PAID'
  AND NOT EXISTS (
    SELECT 1 FROM "user".purchases p
    WHERE p.user_in = o.user_in AND p.unit_id = o.package_id
      AND p.created_at >= o.created_date - interval '10 min'
      AND p.created_at <= o.created_date + interval '30 min')
ON CONFLICT (id) DO NOTHING;

\echo '>>> APPLY: "user".purchases (xarid jurnali)'
INSERT INTO "user".purchases
  (id, amount, user_in, promo, payment_method, unit_type, unit_id,
   created_by, updated_by, created_at, updated_at)
SELECT
  uuid_generate_v5(uuid_ns_url(), 'hesap:backfill-purchase:' || o.id),
  o.amount,
  o.user_in,
  o.promo_code,
  o.provider,
  'PACKAGE',
  o.package_id,
  o.unique_id,
  o.unique_id,
  now(),
  now()
FROM integration.payment_order o
WHERE o.status = 'PAID'
  AND NOT EXISTS (
    SELECT 1 FROM "user".purchases p
    WHERE p.user_in = o.user_in AND p.unit_id = o.package_id
      AND p.created_at >= o.created_date - interval '10 min'
      AND p.created_at <= o.created_date + interval '30 min')
ON CONFLICT (id) DO NOTHING;

\echo '=== YAKUN: bugungi purchases ==='
SELECT id, amount, user_in, payment_method, unit_type, created_at
FROM "user".purchases
WHERE created_at::date = now()::date
ORDER BY created_at DESC;

\else
\echo 'PREVIEW rejimi — yozilmadi. Yozish uchun commit sarlavhasiga [apply] qo-shing.'
\endif
