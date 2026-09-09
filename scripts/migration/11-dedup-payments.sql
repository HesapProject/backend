-- To'lov jadvali dublikatlarini tozalash (soft-delete, qaytarib bo'ladi).
-- Guruh: bir shartnomada bir xil (sana, summa, valyuta).
-- Saqlanadi: to'langani > boshqa yozuv ishora qilgani > eng eskisi.
-- XAVFSIZLIK: guruhda bittadan ko'p "muhim" qator (to'langan yoki ishora qilingan)
-- bo'lsa — TEGILMAYDI, qo'lda ko'rib chiqish uchun hisobotga chiqadi.
-- :apply=false -> faqat PREVIEW.
CREATE SCHEMA IF NOT EXISTS migration;
CREATE TABLE IF NOT EXISTS migration.payment_dedup_log (
  payment_id UUID PRIMARY KEY,
  contract_id UUID,
  contract_payment_date TIMESTAMP,
  total_amount DOUBLE PRECISION,
  kept_id UUID,
  deleted_at TIMESTAMP NOT NULL DEFAULT now()
);

DROP TABLE IF EXISTS dedup_base;
CREATE TEMP TABLE dedup_base AS
SELECT p.id, p.contract_id, p.contract_payment_date, p.total_amount, p.currency,
       p.created_at, p.paid_at,
       (p.paid_at IS NOT NULL OR p.status = 'PAID') AS is_paid,
       (EXISTS (SELECT 1 FROM document.payment_requests r WHERE r.payment_id = p.id AND NOT r.deleted)
        OR EXISTS (SELECT 1 FROM document.delay_requests d WHERE d.payment_id = p.id AND NOT d.deleted)
        OR EXISTS (SELECT 1 FROM document.payment_transactions t WHERE t.payment_schedule_id = p.id AND NOT t.deleted)
       ) AS has_ref
FROM document.payments p
WHERE NOT p.deleted;

DROP TABLE IF EXISTS dedup_rank;
CREATE TEMP TABLE dedup_rank AS
SELECT b.*,
       row_number() OVER w AS rn,
       first_value(b.id) OVER w AS keep_id,
       count(*) OVER (PARTITION BY b.contract_id, b.contract_payment_date, b.total_amount, b.currency) AS grp_size,
       count(*) FILTER (WHERE b.has_ref)
         OVER (PARTITION BY b.contract_id, b.contract_payment_date, b.total_amount, b.currency) AS grp_refs,
       count(*) FILTER (WHERE b.is_paid)
         OVER (PARTITION BY b.contract_id, b.contract_payment_date, b.total_amount, b.currency) AS grp_paid
FROM dedup_base b
WINDOW w AS (PARTITION BY b.contract_id, b.contract_payment_date, b.total_amount, b.currency
             ORDER BY b.has_ref DESC, b.is_paid DESC, b.created_at ASC NULLS LAST, b.id ASC);

\echo '=== Umumiy manzara ==='
SELECT count(*) AS jami_tolov,
       count(*) FILTER (WHERE grp_size > 1) AS dublikatli_qatorlar,
       count(*) FILTER (WHERE grp_size > 1 AND rn > 1) AS ortiqcha_qatorlar
FROM dedup_rank;

\echo '=== O-chiriladi (guruhda tashqi ishora <= 1) ==='
SELECT count(*) FROM dedup_rank WHERE rn > 1 AND grp_refs <= 1;

\echo '=== TEGILMAYDI (guruhda bir nechta TASHQI ISHORA) ==='
SELECT count(*) AS qatorlar, count(DISTINCT contract_id) AS shartnomalar
FROM dedup_rank WHERE rn > 1 AND grp_refs > 1;

\echo '=== Shundan: bir nechta PAID bo-lgan guruhlar (nusxa, saqlanadi bittasi) ==='
SELECT count(*) FILTER (WHERE rn > 1) AS ortiqcha FROM dedup_rank WHERE grp_paid > 1 AND grp_refs <= 1;

\echo '=== Sana NULL bo-lgan guruhlar (alohida e-tibor) ==='
SELECT count(*) FILTER (WHERE rn > 1) AS ortiqcha, count(DISTINCT contract_id) AS shartnomalar
FROM dedup_rank WHERE contract_payment_date IS NULL AND grp_size > 1;

\echo '=== Eng ko-p zararlangan 5 shartnoma ==='
SELECT c.number, count(*) FILTER (WHERE r.rn > 1) AS ortiqcha, count(*) AS jami
FROM dedup_rank r JOIN document.contracts c ON c.id = r.contract_id
GROUP BY 1 HAVING count(*) FILTER (WHERE r.rn > 1) > 0
ORDER BY 2 DESC LIMIT 5;

\if :apply

\echo '>>> APPLY: log yozish'
INSERT INTO migration.payment_dedup_log (payment_id, contract_id, contract_payment_date, total_amount, kept_id)
SELECT id, contract_id, contract_payment_date, total_amount, keep_id
FROM dedup_rank WHERE rn > 1 AND grp_refs <= 1
ON CONFLICT (payment_id) DO NOTHING;

\echo '>>> APPLY: soft-delete'
UPDATE document.payments p
SET deleted = TRUE, updated_at = now()
FROM dedup_rank r
WHERE p.id = r.id AND r.rn > 1 AND r.grp_refs <= 1 AND NOT p.deleted;

\echo '=== YAKUN: to-lovlar holati ==='
SELECT count(*) FILTER (WHERE NOT deleted) AS aktual, count(*) FILTER (WHERE deleted) AS ochirilgan
FROM document.payments;

\echo '=== YAKUN: qolgan dublikatlar ==='
SELECT count(*) FROM (
  SELECT contract_id, contract_payment_date, total_amount, count(*) c
  FROM document.payments WHERE NOT deleted
  GROUP BY 1,2,3 HAVING count(*) > 1) t;

\else
\echo 'PREVIEW — yozilmadi. Yozish uchun [apply] qo-shing.'
\endif
