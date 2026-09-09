-- Payme daromadini haqiqiy manbadan ko'chirish: payment.transaction (status=PAID)
-- -> "user".purchases. Bu money-in jurnali (2025-07 .. 2026-08-26). Avvalgi
-- user_package asosidagi legacy purchase'lar OLIB TASHLANADI (ular balans-sarf,
-- ikki marta sanashga olib kelardi). amount tiyinda -> /100 so'm.
-- user_id -> PINFL: migration.old_users. To'lov usuli: PAYME. Sana: paid_date.
-- Idempotent: deterministik uuid_v5 + ON CONFLICT; DELETE ham deterministik id bo'yicha.
-- :apply=false -> PREVIEW.
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE SCHEMA IF NOT EXISTS migration;

-- Stage: PAID transaction (payment DB'dan eksport)
DROP TABLE IF EXISTS migration.stage_payme_txn;
CREATE TABLE migration.stage_payme_txn (
  txn_id uuid, user_id bigint, user_pnfl text, amount bigint,
  paid_date timestamp, created_date timestamp);
\copy migration.stage_payme_txn FROM '/mig/payme_txn.csv' WITH (FORMAT csv, HEADER true)

-- Stage: eski user_package id'lari (legacy purchase'larni o'chirish uchun)
DROP TABLE IF EXISTS migration.stage_upids;
CREATE TABLE migration.stage_upids (old_id bigint);
\copy migration.stage_upids FROM '/mig/upids.csv' WITH (FORMAT csv, HEADER true)

\echo '=== STAGE: PAID transaction soni ==='
SELECT count(*) FROM migration.stage_payme_txn;

\echo '=== o''chiriladigan legacy user_package purchase soni (hozir mavjud) ==='
SELECT count(*) FROM "user".purchases p
WHERE p.id IN (
  SELECT uuid_generate_v5(uuid_ns_url(), 'hesap:legacy-purchase:' || old_id)
  FROM migration.stage_upids);

\echo '=== PINFL mos kelishi (old_users orqali) ==='
SELECT count(*) FILTER (WHERE ou.pnfl IS NOT NULL AND ou.pnfl<>'') AS pnfl_bor,
       count(*) FILTER (WHERE ou.pnfl IS NULL OR ou.pnfl='') AS pnfl_yoq
FROM migration.stage_payme_txn s
LEFT JOIN migration.old_users ou ON ou.id = s.user_id;

\echo '=== jami daromad (so''m) va yil kesimida ==='
SELECT to_char(SUM(amount)/100.0,'FM999 999 999 990') AS jami_som FROM migration.stage_payme_txn;
SELECT date_part('year', coalesce(paid_date,created_date)) yil, count(*),
       to_char(sum(amount)/100.0,'FM999 999 990') som
FROM migration.stage_payme_txn GROUP BY 1 ORDER BY 1;

\if :apply

\echo '>>> DELETE: avvalgi user_package legacy purchase''lar'
DELETE FROM "user".purchases p
WHERE p.id IN (
  SELECT uuid_generate_v5(uuid_ns_url(), 'hesap:legacy-purchase:' || old_id)
  FROM migration.stage_upids);

\echo '>>> APPLY: "user".purchases (payme transaction PAID)'
INSERT INTO "user".purchases
  (id, amount, user_in, promo, payment_method, unit_type, unit_id,
   created_by, updated_by, created_at, updated_at)
SELECT
  uuid_generate_v5(uuid_ns_url(), 'hesap:payme-txn:' || s.txn_id),
  s.amount / 100.0,
  nullif(ou.pnfl, ''),
  NULL,
  'PAYME',
  'PACKAGE',
  NULL,
  um.new_id,
  um.new_id,
  coalesce(s.paid_date, s.created_date),
  coalesce(s.paid_date, s.created_date)
FROM migration.stage_payme_txn s
LEFT JOIN migration.old_users ou ON ou.id = s.user_id
LEFT JOIN migration.user_id_map um ON um.old_id = s.user_id
ON CONFLICT (id) DO NOTHING;

\echo '=== YAKUN: purchases jami + butun tarix tashqi daromad (so''m) ==='
SELECT count(*) AS purchases_jami FROM "user".purchases;
SELECT to_char(COALESCE(SUM(amount),0),'FM999 999 999 990') AS tashqi_daromad_som
FROM "user".purchases WHERE payment_method NOT IN ('BALANCE','CONTROL');

\else
\echo 'PREVIEW rejimi — yozilmadi. Yozish uchun commit sarlavhasiga [apply] qo-shing.'
\endif
