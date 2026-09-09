-- 15-dan keyin qolgan qatorlar: paid_amount jadval summasidan bir necha yuz/ming
-- barobar katta (100 ga bo-lish ham yordam bermaydi) — migratsiyada buzilgan.
-- PAID bo-lgani uchun to-g-ri qiymat = total_amount.
CREATE TABLE IF NOT EXISTS migration.paid_impossible_fix_log (
  payment_id UUID PRIMARY KEY, contract_id UUID,
  old_paid DOUBLE PRECISION, new_paid DOUBLE PRECISION,
  status TEXT, fixed_at TIMESTAMP NOT NULL DEFAULT now()
);
DROP TABLE IF EXISTS impfix;
CREATE TEMP TABLE impfix AS
SELECT p.id, p.contract_id, p.paid_amount AS old_paid, p.total_amount AS new_paid, p.status::text AS status
FROM document.payments p
WHERE NOT p.deleted AND coalesce(p.paid_amount,0) > 0 AND p.total_amount > 0
  AND p.paid_amount > p.total_amount * 1.01
  AND p.paid_amount / 100 > p.total_amount * 1.01;

\echo '=== Statuslar bo-yicha ==='
SELECT status, count(*) FROM impfix GROUP BY 1 ORDER BY 2 DESC;

\if :apply
INSERT INTO migration.paid_impossible_fix_log (payment_id, contract_id, old_paid, new_paid, status)
SELECT id, contract_id, old_paid, new_paid, status FROM impfix ON CONFLICT (payment_id) DO NOTHING;
UPDATE document.payments p SET paid_amount = f.new_paid, updated_at = now()
FROM impfix f WHERE p.id = f.id AND f.status = 'PAID';
\echo '=== YAKUN: qolgan imkonsiz qatorlar ==='
SELECT count(*) FROM document.payments
WHERE NOT deleted AND total_amount > 0 AND coalesce(paid_amount,0) > total_amount * 1.01;
\else
\echo 'PREVIEW — yozilmadi.'
\endif
