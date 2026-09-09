-- Eski tizim prepare-id to'lov kollisiyasini tuzatish.
-- Haqiqiy jadval = trust.payment WHERE contract_id = contract.prepare_id
-- (shartnoma bilan bir vaqt oralig'ida yaratilgan). Begona = contract_id=c.id
-- bo'lib shartnomadan >1 soat OLDIN yaratilganlar (id qayta ishlatilgan).
-- has_own=true bo'lganlar (o'z id ostida ham shu vaqt oralig'ida jadvali borlar)
-- TEGILMAYDI — ularning jadvali to'g'ri, prepare qatorlari dublikat.
-- :apply='true' bo'lsa yozadi; aks holda PREVIEW.

CREATE SCHEMA IF NOT EXISTS migration;
CREATE TABLE IF NOT EXISTS migration.fix_pp_real(
  old_contract_id bigint, old_payment_id bigint, arrangement_at timestamp,
  is_paid text, paid_at timestamp, price text, paid text, status text,
  currency_id text, created_at timestamp, has_own text);
CREATE TABLE IF NOT EXISTS migration.fix_pp_foreign(old_payment_id bigint, old_contract_id bigint);
TRUNCATE migration.fix_pp_real, migration.fix_pp_foreign;
\copy migration.fix_pp_real FROM '/mig/fix_real.csv' CSV HEADER
\copy migration.fix_pp_foreign FROM '/mig/fix_foreign.csv' CSV HEADER

UPDATE migration.fix_pp_real SET
  is_paid = CASE lower(is_paid) WHEN 't' THEN 'true' WHEN 'f' THEN 'false' ELSE lower(coalesce(is_paid,'')) END,
  has_own = CASE lower(has_own) WHEN 't' THEN 'true' WHEN 'f' THEN 'false' ELSE lower(coalesce(has_own,'')) END;

\echo '=== o''z jadvali ham bor (tegilmaydi, dublikat prepare qatorlar) — shartnoma soni ==='
SELECT count(DISTINCT old_contract_id) FROM migration.fix_pp_real WHERE has_own='true';

\echo '=== tuzatiladigan yangi-DB shartnomalar soni ==='
SELECT count(DISTINCT m.new_id)
FROM migration.contract_id_map m
JOIN migration.fix_pp_real r ON r.old_contract_id=m.old_id AND r.has_own<>'true';

\echo '=== begona to''lovlar: yangi DB''da o''chiriladiganlar ==='
SELECT count(*)
FROM document.payments dp
JOIN migration.fix_pp_foreign f
  ON dp.id = uuid_generate_v5(uuid_ns_url(),'hesap:old-payment:'||f.old_payment_id)
JOIN migration.contract_id_map m ON m.old_id=f.old_contract_id AND dp.contract_id=m.new_id;

\echo '=== haqiqiy to''lovlar: boshqa joyda turibdi (RELINK qilinadi) ==='
SELECT count(*)
FROM document.payments dp
JOIN migration.fix_pp_real r
  ON dp.id = uuid_generate_v5(uuid_ns_url(),'hesap:old-payment:'||r.old_payment_id)
JOIN migration.contract_id_map m ON m.old_id=r.old_contract_id
WHERE r.has_own<>'true' AND dp.contract_id <> m.new_id;

\echo '=== haqiqiy to''lovlar: yangidan INSERT qilinadiganlar ==='
SELECT count(*)
FROM migration.fix_pp_real r
JOIN migration.contract_id_map m ON m.old_id=r.old_contract_id
WHERE r.has_own<>'true'
  AND NOT EXISTS (SELECT 1 FROM document.payments dp
                  WHERE dp.id=uuid_generate_v5(uuid_ns_url(),'hesap:old-payment:'||r.old_payment_id));

\echo '=== mijoz keysi 260815-0131: tiklanadigan jadval ==='
SELECT r.arrangement_at, nullif(r.price,'')::numeric/100 AS som, r.is_paid
FROM migration.fix_pp_real r
JOIN migration.contract_id_map m ON m.old_id=r.old_contract_id
JOIN document.contracts dc ON dc.id=m.new_id
WHERE replace(dc.number,'-','')='2608150131' ORDER BY 1;

\if :apply
\echo '>>> APPLY'
BEGIN;

-- 1) begona to'lovlarni o'chirish
DELETE FROM document.payments dp
USING migration.fix_pp_foreign f, migration.contract_id_map m
WHERE m.old_id = f.old_contract_id
  AND dp.id = uuid_generate_v5(uuid_ns_url(),'hesap:old-payment:'||f.old_payment_id)
  AND dp.contract_id = m.new_id;

-- 2) haqiqiy to'lov allaqachon bor (raqamli kollisiya tufayli boshqa shartnomaga
--    ulangan) — to'g'ri shartnomaga RELINK
UPDATE document.payments dp
SET contract_id = m.new_id, buyer_in = dc.buyer_in, seller_in = dc.seller_in,
    contract_status = dc.status, updated_at = now()
FROM migration.fix_pp_real r
JOIN migration.contract_id_map m ON m.old_id = r.old_contract_id
JOIN document.contracts dc ON dc.id = m.new_id
WHERE r.has_own <> 'true'
  AND dp.id = uuid_generate_v5(uuid_ns_url(),'hesap:old-payment:'||r.old_payment_id)
  AND dp.contract_id <> m.new_id;

-- 3) yetishmayotgan haqiqiy to'lovlarni INSERT
INSERT INTO document.payments
 (id, contract_id, buyer_in, seller_in, status, contract_status, total_amount,
  paid_amount, currency, currency_id, contract_payment_date, changed_payment_date,
  paid_at, deleted, created_at, updated_at)
SELECT uuid_generate_v5(uuid_ns_url(),'hesap:old-payment:'||r.old_payment_id),
  m.new_id, dc.buyer_in, dc.seller_in,
  CASE WHEN r.is_paid='true' OR upper(coalesce(r.status,''))='FULL' THEN 'PAID' ELSE 'PENDING' END,
  dc.status, nullif(r.price,'')::numeric,
  coalesce(nullif(r.paid,'')::numeric,
           CASE WHEN r.is_paid='true' THEN nullif(r.price,'')::numeric END, 0),
  dc.currency, dc.currency_id,
  r.arrangement_at, NULL, r.paid_at,
  false, coalesce(r.created_at, now()), now()
FROM migration.fix_pp_real r
JOIN migration.contract_id_map m ON m.old_id = r.old_contract_id
JOIN document.contracts dc ON dc.id = m.new_id
WHERE r.has_own <> 'true'
ON CONFLICT (id) DO NOTHING;

COMMIT;

\echo '=== APPLY dan keyin: mijoz keysi jadvali ==='
SELECT p.contract_payment_date, p.status, p.total_amount/100 AS som
FROM document.payments p
JOIN document.contracts dc ON dc.id = p.contract_id
WHERE replace(dc.number,'-','')='2608150131' AND p.deleted=false
ORDER BY 1;
\endif
