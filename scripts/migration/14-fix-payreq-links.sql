-- To'lov so'rovlari noto'g'ri to'lovga bog'langan: migratsiyada pay_id_map "eng yaqin
-- sana" bo'yicha tanlagan (eski payment.contract_id ba'zan prepare id bo'lgani uchun).
-- Bu skript qat'iy moslash bilan qayta bog'laydi: eski payment -> (shartnoma, kun, summa).
-- Mos kelmaganlari soft-delete. amount=0 (eski "to'liq to'lov" semantikasi) to'lov
-- summasiga to'ldiriladi. :apply=false -> PREVIEW.
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE TABLE IF NOT EXISTS migration.payreq_fix_log (
  request_id UUID PRIMARY KEY, kind TEXT,
  old_payment_id UUID, new_payment_id UUID,
  old_contract_id UUID, new_contract_id UUID,
  old_amount DOUBLE PRECISION, new_amount DOUBLE PRECISION,
  action TEXT, fixed_at TIMESTAMP NOT NULL DEFAULT now()
);

-- 1) Eski to'lov id -> yangi to'lov (qat'iy: shartnoma + kun + summa)
DROP TABLE IF EXISTS paymap;
-- Eski to'lov id -> yangi to'lov. `contract_id` eski bazada ham SHARTNOMA, ham
-- QORALAMA (prepare) id bo'lishi mumkin (id fazosi umumiy) — shuning uchun ikkala
-- nomzodni ham olamiz va sana+summa bo'yicha eng mos keladiganini tanlaymiz.
CREATE TEMP TABLE paymap AS
SELECT DISTINCT ON (lp.id) lp.id AS old_payment_id, p.id AS new_payment_id,
       p.contract_id, p.total_amount, p.contract_payment_date
FROM migration.legacy_payment lp
JOIN LATERAL (
  -- nomzod 1: contract_id shartnoma id sifatida
  SELECT cm.new_id, 1 AS pref
  FROM migration.contract_id_map cm WHERE cm.old_id::text = lp.contract_id::text
  UNION ALL
  -- nomzod 2: contract_id qoralama (prepare) id sifatida
  SELECT cm2.new_id, 2 AS pref
  FROM migration.prepare_map pm
  JOIN migration.contract_id_map cm2 ON cm2.old_id::text = pm.contract_id::text
  WHERE pm.prepare_id::text = lp.contract_id::text
) cand ON true
JOIN document.payments p
  ON p.contract_id = cand.new_id AND NOT p.deleted
 AND date_trunc('day', p.contract_payment_date)
     = date_trunc('day', nullif(lp.arrangement_at,'')::timestamp)
ORDER BY lp.id,
         -- avval summasi aynan mos keladigani, keyin eng yaqin summa, keyin domen tartibi
         (p.total_amount = nullif(lp.price,'')::numeric) DESC,
         abs(p.total_amount - coalesce(nullif(lp.price,'')::numeric,0)) ASC,
         cand.pref ASC;

\echo '=== paymap (eski to-lov -> yangi to-lov) ==='
SELECT count(*) FROM paymap;

-- 2) Migratsiyadan kelgan to'lov so'rovlari uchun to'g'ri bog'lanish
DROP TABLE IF EXISTS reqfix;
CREATE TEMP TABLE reqfix AS
SELECT uuid_generate_v5(uuid_ns_url(),'hesap:old-payreq:'||s.id) AS request_id,
       'PAYMENT'::text AS kind,
       pm.new_payment_id, pm.contract_id, pm.total_amount, pm.contract_payment_date,
       nullif(s.amount,'')::numeric AS req_amount_som
FROM migration.stage_payreq s
JOIN paymap pm ON pm.old_payment_id::text = s.payment_id::text
WHERE s.is_payment IN ('t','true');

DROP TABLE IF EXISTS delayfix;
CREATE TEMP TABLE delayfix AS
SELECT uuid_generate_v5(uuid_ns_url(),'hesap:old-delayreq:'||s.id) AS request_id,
       'DELAY'::text AS kind,
       pm.new_payment_id, pm.contract_id, pm.total_amount, pm.contract_payment_date
FROM migration.stage_payreq s
JOIN paymap pm ON pm.old_payment_id::text = s.payment_id::text
WHERE s.is_date_change IN ('t','true');

\echo '=== To-lov so-rovlari holati ==='
SELECT
  count(*) FILTER (WHERE f.request_id IS NOT NULL AND r.payment_id = f.new_payment_id) AS togri_bogliq,
  count(*) FILTER (WHERE f.request_id IS NOT NULL AND r.payment_id IS DISTINCT FROM f.new_payment_id) AS qayta_boglanadi,
  count(*) FILTER (WHERE f.request_id IS NULL AND r.created_date >= c.created_date) AS moslik_yoq_qoladi,
  count(*) FILTER (WHERE f.request_id IS NULL AND r.created_date < c.created_date
                   AND EXISTS (SELECT 1 FROM migration.contract_id_map m2
                               JOIN migration.stage_c3u s2 ON s2.id = m2.old_id
                               WHERE m2.new_id = c.id
                                 AND date_trunc('day', c.created_date)
                                     = date_trunc('day', nullif(s2.created_at,'')::timestamp)))
    AS isbotlangan_xato_ochiriladi,
  count(*) FILTER (WHERE f.request_id IS NULL AND r.created_date < c.created_date
                   AND NOT EXISTS (SELECT 1 FROM migration.contract_id_map m2
                                   JOIN migration.stage_c3u s2 ON s2.id = m2.old_id
                                   WHERE m2.new_id = c.id
                                     AND date_trunc('day', c.created_date)
                                         = date_trunc('day', nullif(s2.created_at,'')::timestamp)))
    AS sanasi_ishonchsiz_tegilmaydi
FROM document.payment_requests r
JOIN document.contracts c ON c.id = r.contract_id
LEFT JOIN reqfix f ON f.request_id = r.id
WHERE NOT r.deleted
  AND EXISTS (SELECT 1 FROM migration.contract_id_map m WHERE m.new_id = r.contract_id);

\echo '=== Kechiktirish so-rovlari holati ==='
SELECT
  count(*) FILTER (WHERE f.request_id IS NOT NULL AND d.payment_id = f.new_payment_id) AS togri_bogliq,
  count(*) FILTER (WHERE f.request_id IS NOT NULL AND d.payment_id IS DISTINCT FROM f.new_payment_id) AS qayta_boglanadi,
  count(*) FILTER (WHERE f.request_id IS NULL AND d.created_date >= c.created_date) AS moslik_yoq_qoladi,
  count(*) FILTER (WHERE f.request_id IS NULL AND d.created_date < c.created_date) AS isbotlangan_xato_ochiriladi
FROM document.delay_requests d
JOIN document.contracts c ON c.id = d.contract_id
LEFT JOIN delayfix f ON f.request_id = d.id
WHERE NOT d.deleted
  AND EXISTS (SELECT 1 FROM migration.contract_id_map m WHERE m.new_id = d.contract_id);

\echo '=== amount=0 (eski "to-liq to-lov") — to-lov summasiga to-ldiriladi ==='
SELECT count(*) FROM document.payment_requests r
JOIN reqfix f ON f.request_id = r.id
WHERE NOT r.deleted AND (r.amount IS NULL OR r.amount = 0) AND f.total_amount > 0;

\if :apply

\echo '>>> APPLY: to-lov so-rovlarini qayta bog-lash'
INSERT INTO migration.payreq_fix_log
  (request_id, kind, old_payment_id, new_payment_id, old_contract_id, new_contract_id,
   old_amount, new_amount, action)
SELECT r.id, 'PAYMENT', r.payment_id, f.new_payment_id, r.contract_id, f.contract_id,
       r.amount, CASE WHEN coalesce(r.amount,0)=0 THEN f.total_amount ELSE r.amount END,
       'RELINK'
FROM document.payment_requests r JOIN reqfix f ON f.request_id = r.id
WHERE NOT r.deleted
  AND (r.payment_id IS DISTINCT FROM f.new_payment_id
       OR r.contract_id IS DISTINCT FROM f.contract_id
       OR coalesce(r.amount,0) = 0)
ON CONFLICT (request_id) DO NOTHING;

UPDATE document.payment_requests r
SET payment_id = f.new_payment_id,
    contract_id = f.contract_id,
    payment_date = f.contract_payment_date,
    amount = CASE WHEN coalesce(r.amount,0) = 0 THEN f.total_amount ELSE r.amount END,
    buyer_in = c.buyer_in,
    seller_in = c.seller_in,
    last_modified_date = now()
FROM reqfix f JOIN document.contracts c ON c.id = f.contract_id
WHERE r.id = f.request_id AND NOT r.deleted;

\echo '>>> APPLY: kechiktirish so-rovlarini qayta bog-lash'
UPDATE document.delay_requests d
SET payment_id = f.new_payment_id,
    contract_id = f.contract_id,
    buyer_in = c.buyer_in,
    seller_in = c.seller_in,
    last_modified_date = now()
FROM delayfix f JOIN document.contracts c ON c.id = f.contract_id
WHERE d.id = f.request_id AND NOT d.deleted;

\echo '>>> APPLY: mosligi yo-q (yetim) so-rovlarni soft-delete'
INSERT INTO migration.payreq_fix_log
  (request_id, kind, old_payment_id, new_payment_id, old_contract_id, new_contract_id,
   old_amount, new_amount, action)
SELECT r.id, 'PAYMENT', r.payment_id, NULL, r.contract_id, NULL, r.amount, NULL, 'DELETED'
FROM document.payment_requests r JOIN document.contracts c ON c.id = r.contract_id
WHERE NOT r.deleted
  AND EXISTS (SELECT 1 FROM migration.contract_id_map m WHERE m.new_id = r.contract_id)
  AND NOT EXISTS (SELECT 1 FROM reqfix f WHERE f.request_id = r.id)
  AND r.created_date < c.created_date
  AND EXISTS (SELECT 1 FROM migration.contract_id_map m2
              JOIN migration.stage_c3u s2 ON s2.id = m2.old_id
              WHERE m2.new_id = c.id
                AND date_trunc('day', c.created_date)
                    = date_trunc('day', nullif(s2.created_at,'')::timestamp))
ON CONFLICT (request_id) DO NOTHING;

UPDATE document.payment_requests r SET deleted = TRUE, last_modified_date = now()
FROM document.contracts c
WHERE c.id = r.contract_id AND NOT r.deleted
  AND EXISTS (SELECT 1 FROM migration.contract_id_map m WHERE m.new_id = r.contract_id)
  AND NOT EXISTS (SELECT 1 FROM reqfix f WHERE f.request_id = r.id)
  AND r.created_date < c.created_date
  AND EXISTS (SELECT 1 FROM migration.contract_id_map m2
              JOIN migration.stage_c3u s2 ON s2.id = m2.old_id
              WHERE m2.new_id = c.id
                AND date_trunc('day', c.created_date)
                    = date_trunc('day', nullif(s2.created_at,'')::timestamp));

UPDATE document.delay_requests d SET deleted = TRUE, last_modified_date = now()
FROM document.contracts c
WHERE c.id = d.contract_id AND NOT d.deleted
  AND EXISTS (SELECT 1 FROM migration.contract_id_map m WHERE m.new_id = d.contract_id)
  AND NOT EXISTS (SELECT 1 FROM delayfix f WHERE f.request_id = d.id)
  AND d.created_date < c.created_date
  AND EXISTS (SELECT 1 FROM migration.contract_id_map m2
              JOIN migration.stage_c3u s2 ON s2.id = m2.old_id
              WHERE m2.new_id = c.id
                AND date_trunc('day', c.created_date)
                    = date_trunc('day', nullif(s2.created_at,'')::timestamp));

\echo '=== YAKUN: mantiqsiz sanalar qoldimi? ==='
SELECT count(*) FROM document.payment_requests r JOIN document.contracts c ON c.id=r.contract_id
WHERE NOT r.deleted AND r.created_date < c.created_date;
SELECT count(*) AS nol_summa FROM document.payment_requests WHERE NOT deleted AND coalesce(amount,0)=0;
\else
\echo 'PREVIEW — yozilmadi.'
\endif
