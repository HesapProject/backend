-- x100 skripti (2026-07-18) allaqachon TIYIN da bo'lgan summalarni yana 100 ga
-- ko'paytirgan. Eski backend `currencyFormat()` = value/100 — ya'ni eski baza
-- tiyinda saqlagan, x100 ortiqcha edi.
-- Bu skript faqat aniq mos keladigan qatorlarni tuzatadi:
--   price = eski_xom_summa * 100  ->  price = eski_xom_summa
-- initial_payment ham shu shartda qaytariladi. To'lovlar tekshiriladi (deyarli
-- hammasi allaqachon to'g'ri). :apply=false -> PREVIEW.
CREATE TABLE IF NOT EXISTS migration.price_x100_fix_log (
  contract_id UUID PRIMARY KEY, number TEXT,
  old_price NUMERIC, new_price NUMERIC,
  old_initial NUMERIC, new_initial NUMERIC,
  fixed_at TIMESTAMP NOT NULL DEFAULT now()
);

DROP TABLE IF EXISTS fixset;
CREATE TEMP TABLE fixset AS
SELECT c.id, c.number, c.price::numeric AS cur_price, s.amount::numeric AS true_price,
       c.initial_payment::numeric AS cur_initial
FROM document.contracts c
JOIN migration.contract_id_map m ON m.new_id = c.id
JOIN migration.stage_c3u s ON s.id = m.old_id
WHERE NOT c.deleted
  AND s.amount ~ '^[0-9.]+$' AND s.amount::numeric > 0
  AND c.price::numeric = s.amount::numeric * 100;

\echo '=== Tuzatiladigan shartnomalar ==='
SELECT count(*) AS soni,
       (min(true_price)/100)::numeric(18,2) AS eng_kichik_som,
       (max(true_price)/100)::numeric(18,2) AS eng_katta_som
FROM fixset;

\echo '=== Namuna (hozirgi -> to-g-ri) ==='
SELECT number, (cur_price/100)::numeric(18,2) AS hozir_som,
       (true_price/100)::numeric(18,2) AS togri_som,
       (SELECT (sum(p.total_amount)/100)::numeric(18,2) FROM document.payments p
        WHERE p.contract_id=f.id AND NOT p.deleted) AS tolovlar_som
FROM fixset f ORDER BY true_price DESC LIMIT 10;

\echo '=== Tuzatilgandan keyin price va to-lovlar mos keladimi? ==='
SELECT CASE WHEN pay_sum IS NULL THEN 'to-lovi yo-q'
            WHEN abs(true_price - pay_sum) <= greatest(true_price,pay_sum)*0.02 THEN 'MOS'
            ELSE 'farq bor' END AS holat, count(*)
FROM (SELECT f.*, (SELECT sum(p.total_amount) FROM document.payments p
                   WHERE p.contract_id=f.id AND NOT p.deleted) AS pay_sum FROM fixset f) x
GROUP BY 1 ORDER BY 2 DESC;

\if :apply
\echo '>>> APPLY: log'
INSERT INTO migration.price_x100_fix_log (contract_id, number, old_price, new_price, old_initial, new_initial)
SELECT id, number, cur_price, true_price, cur_initial,
       CASE WHEN cur_initial IS NULL THEN NULL ELSE cur_initial/100 END
FROM fixset ON CONFLICT (contract_id) DO NOTHING;

\echo '>>> APPLY: price tuzatish'
UPDATE document.contracts c
SET price = f.true_price,
    initial_payment = CASE WHEN c.initial_payment IS NULL THEN NULL ELSE c.initial_payment/100 END,
    last_modified_date = now()
FROM fixset f WHERE c.id = f.id;

\echo '=== YAKUN: qolgan 100x nomutanosiblik ==='
SELECT count(*) FROM document.contracts c
JOIN migration.contract_id_map m ON m.new_id=c.id
JOIN migration.stage_c3u s ON s.id=m.old_id
WHERE NOT c.deleted AND s.amount ~ '^[0-9.]+$' AND s.amount::numeric > 0
  AND c.price::numeric = s.amount::numeric * 100;
\else
\echo 'PREVIEW — yozilmadi.'
\endif
