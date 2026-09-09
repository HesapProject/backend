-- 09-delta-updates.sql xatosi: paid_amount ga ortiqcha *100 qo'llangan (eski baza
-- allaqachon tiyinda). Natijada PDF/ilovada "to'langan" summa 100 barobar katta.
-- Faqat isbotlanadigan qatorlar tuzatiladi: to'langan summa jadval summasidan
-- katta, lekin 100 ga bo'linsa mos tushadi. :apply=false -> PREVIEW.
CREATE TABLE IF NOT EXISTS migration.paid_x100_fix_log (
  payment_id UUID PRIMARY KEY, contract_id UUID,
  old_paid DOUBLE PRECISION, new_paid DOUBLE PRECISION,
  total_amount DOUBLE PRECISION, fixed_at TIMESTAMP NOT NULL DEFAULT now()
);

DROP TABLE IF EXISTS paidfix;
CREATE TEMP TABLE paidfix AS
SELECT p.id, p.contract_id, p.paid_amount AS old_paid,
       p.paid_amount / 100 AS new_paid, p.total_amount
FROM document.payments p
WHERE NOT p.deleted
  AND coalesce(p.paid_amount,0) > 0
  AND p.total_amount > 0
  AND p.paid_amount > p.total_amount * 1.01          -- imkonsiz ortiqcha to'lov
  AND p.paid_amount / 100 <= p.total_amount * 1.01;  -- 100 ga bo'linsa mos tushadi

\echo '=== Tuzatiladigan to-lovlar ==='
SELECT count(*) AS soni, count(DISTINCT contract_id) AS shartnomalar,
       (min(new_paid)/100)::numeric(18,2) AS eng_kichik_som,
       (max(new_paid)/100)::numeric(18,2) AS eng_katta_som
FROM paidfix;

\echo '=== Tegilmaydi: 100 ga bo-lingandan keyin ham katta (qo-lda ko-rish) ==='
SELECT count(*) FROM document.payments p
WHERE NOT p.deleted AND coalesce(p.paid_amount,0) > 0 AND p.total_amount > 0
  AND p.paid_amount > p.total_amount * 1.01
  AND p.paid_amount / 100 > p.total_amount * 1.01;

\if :apply
\echo '>>> APPLY: log'
INSERT INTO migration.paid_x100_fix_log (payment_id, contract_id, old_paid, new_paid, total_amount)
SELECT id, contract_id, old_paid, new_paid, total_amount FROM paidfix
ON CONFLICT (payment_id) DO NOTHING;

\echo '>>> APPLY: paid_amount tuzatish'
UPDATE document.payments p
SET paid_amount = f.new_paid, updated_at = now()
FROM paidfix f WHERE p.id = f.id;

\echo '=== YAKUN: qolgan imkonsiz qatorlar ==='
SELECT count(*) FROM document.payments
WHERE NOT deleted AND total_amount > 0 AND coalesce(paid_amount,0) > total_amount * 1.01;
\else
\echo 'PREVIEW — yozilmadi.'
\endif
