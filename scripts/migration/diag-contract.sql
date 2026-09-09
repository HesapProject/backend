-- diag-contract.sql — bitta shartnoma (num) to'liq TASHXIS (READ-ONLY, hech nima yozmaydi).
-- Shartnoma narxi, mahsulotlar, to'lovlar va ESKI MANBA (stage_c3u) bilan solishtiradi —
-- "suma notog'ri" ning sababini (×100, nomuvofiqlik, valyuta) aniqlash uchun.
-- Ishga tushirish: psql ... -v num=4706 -f diag-contract.sql

\if :{?num}
\else
  \set num '4706'
\endif

\echo '===================================================================='
\echo 'TASHXIS: shartnoma №' :'num'
\echo '===================================================================='

\echo '--- Shartnoma ---'
SELECT id, number, status, currency,
       (price::numeric/100)::numeric(18,2)           AS narx_som,
       (initial_payment::numeric/100)::numeric(18,2) AS boshlangich_som,
       created_date::date AS yaratilgan
FROM document.contracts WHERE number = :'num' AND NOT deleted;

\echo '--- Mahsulotlar (products) ---'
SELECT pr.name, (pr.amount::numeric/100)::numeric(18,2) AS summa_som
FROM document.products pr
JOIN document.contracts c ON c.id = pr.document_id
WHERE c.number = :'num' AND NOT pr.deleted;

\echo '--- Solishtirish: narx vs mahsulot jami vs to''lovlar jami (so''m) ---'
SELECT (c.price::numeric/100)::numeric(18,2) AS shartnoma_narx,
       (COALESCE((SELECT sum(pr.amount) FROM document.products pr WHERE pr.document_id=c.id AND NOT pr.deleted),0)::numeric/100)::numeric(18,2) AS mahsulot_jami,
       (COALESCE((SELECT sum(p.total_amount) FROM document.payments p WHERE p.contract_id=c.id AND NOT p.deleted),0)/100.0)::numeric(18,2) AS tolovlar_jami,
       (SELECT count(*) FROM document.payments p WHERE p.contract_id=c.id AND NOT p.deleted) AS tolovlar_soni
FROM document.contracts c WHERE c.number = :'num' AND NOT c.deleted;

\echo '--- To''lovlar ---'
SELECT p.contract_payment_date::date AS sana,
       (p.total_amount/100.0)::numeric(18,2) AS som, p.currency, p.status
FROM document.payments p JOIN document.contracts c ON c.id=p.contract_id
WHERE c.number = :'num' AND NOT p.deleted
ORDER BY p.contract_payment_date;

\echo '--- ESKI MANBA (stage_c3u) bilan solishtirish — ×100 tekshiruvi ---'
-- narx_div_old = yangi_narx(tiyin) / eski_amount. 1=to''g''ri, 100=×100 bug (real=eski),
-- 0.01=÷100 kerak. Eski amount stage_c3u da (odatda tiyin).
SELECT s.id AS old_id, s.amount AS eski_amount_xom,
       (c.price::numeric)         AS yangi_narx_tiyin,
       CASE WHEN s.amount ~ '^[0-9.]+$' AND s.amount::numeric > 0
            THEN round(c.price::numeric / s.amount::numeric, 4)::text
            ELSE 'eski raqam emas' END AS narx_bulinma_eski,
       (s.amount::numeric/100)::numeric(18,2) AS eski_som_agar_tiyin
FROM document.contracts c
JOIN migration.contract_id_map m ON m.new_id = c.id
JOIN migration.stage_c3u s ON s.id = m.old_id
WHERE c.number = :'num' AND NOT c.deleted;
