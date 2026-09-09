-- Delta UPDATE sinxron: eski appda migratsiyadan keyin O'ZGARGAN yozuvlar.
-- Faqat yangi tizimda hali tegilmagan qatorlar yangilanadi (yangi appdagi
-- o'zgarishlar ustidan yozilmaydi). :apply=false -> PREVIEW.

\echo '=== PREVIEW: status o-zgargan shartnomalar (yangi=ACTIVE, eski yopilgan) ==='
SELECT count(*) FROM migration.stage_c3u s
JOIN migration.contract_id_map m ON m.old_id=s.id
JOIN document.contracts dc ON dc.id=m.new_id
WHERE dc.status='ACTIVE'
  AND CASE upper(coalesce(s.status,''))
        WHEN 'CLOSE' THEN 'COMPLETED' WHEN 'CLOSED' THEN 'COMPLETED'
        WHEN 'CANCELLED' THEN 'CANCELLED' WHEN 'REJECTED' THEN 'REJECTED' END IS NOT NULL;

\echo '=== PREVIEW: eski appda to-langan, yangi tizimda hali PENDING to-lovlar ==='
SELECT count(*) FROM migration.legacy_payment lp
JOIN migration.pay_id_map pm ON pm.old_payment_id=lp.id
JOIN document.payments p ON p.id=pm.new_payment_id
WHERE p.paid_at IS NULL
  AND (lower(coalesce(lp.is_paid,'')) IN ('t','true') OR nullif(lp.paid_at,'') IS NOT NULL);

\if :apply

\echo '>>> APPLY: shartnoma statuslari (ACTIVE -> eski yakuniy status)'
UPDATE document.contracts dc
SET status = x.new_status, last_modified_date = now()
FROM (
  SELECT m.new_id,
    CASE upper(coalesce(s.status,''))
      WHEN 'CLOSE' THEN 'COMPLETED' WHEN 'CLOSED' THEN 'COMPLETED'
      WHEN 'CANCELLED' THEN 'CANCELLED' WHEN 'REJECTED' THEN 'REJECTED' END AS new_status
  FROM migration.stage_c3u s
  JOIN migration.contract_id_map m ON m.old_id=s.id
) x
WHERE dc.id = x.new_id AND x.new_status IS NOT NULL AND dc.status='ACTIVE';

\echo '>>> APPLY: to-lovlar paid backfill (yangi tizimda paid_at IS NULL bo-lganlar)'
UPDATE document.payments p
SET status = 'PAID',
    paid_at = coalesce(nullif(lp.paid_at,'')::timestamp, now()),
    -- Eski baza TIYIN da saqlaydi — ko'paytirish SHART EMAS (avval *100 xato edi).
    paid_amount = coalesce(nullif(lp.paid,'')::numeric, nullif(lp.price,'')::numeric, 0),
    updated_at = now()
FROM migration.legacy_payment lp
JOIN migration.pay_id_map pm ON pm.old_payment_id=lp.id
WHERE p.id = pm.new_payment_id
  AND p.paid_at IS NULL
  AND (lower(coalesce(lp.is_paid,'')) IN ('t','true') OR nullif(lp.paid_at,'') IS NOT NULL);

\echo '=== YAKUN: paid/pending taqsimot ==='
SELECT status, count(*) FROM document.payments GROUP BY 1 ORDER BY 2 DESC LIMIT 6;

\else
\echo 'PREVIEW rejimi — yozilmadi.'
\endif
