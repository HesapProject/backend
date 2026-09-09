-- 07: Eski (old hesap) shartnoma summalarini so'mdan TIYINGA (x100) o'tkazish.
-- 04-contract-recover / 05-payment-witness summalarni so'mligicha ko'chirgan,
-- yangi backend esa hamma summani tiyinda saqlaydi — eski shartnomalar 100 barobar
-- kichik ko'rinardi.
--
-- :apply = 'true' bo'lsa UPDATE qiladi; aks holda faqat PREVIEW (yozmaydi).
-- Idempotent: migration.x100_contracts_done marker jadvali — ikkinchi marta
-- ishga tushsa ham qayta ko'paytirmaydi.
BEGIN;

CREATE SCHEMA IF NOT EXISTS migration;
CREATE TABLE IF NOT EXISTS migration.x100_contracts_done (
  applied_at timestamptz NOT NULL DEFAULT now()
);

\echo '=== PREVIEW: eski shartnomalar ==='
SELECT count(*) AS old_contracts FROM migration.contract_id_map;
SELECT count(*) AS already_applied FROM migration.x100_contracts_done;

\echo '--- document.contracts (price, so''mda bo''lsa kichik ko''rinadi) ---'
SELECT count(*) AS cnt, min(price) AS min_price, max(price) AS max_price
FROM document.contracts
WHERE id IN (SELECT new_id FROM migration.contract_id_map) AND price IS NOT NULL;

\echo '--- document.payments (total_amount/paid_amount) ---'
SELECT count(*) AS cnt, min(total_amount) AS min_total, max(total_amount) AS max_total
FROM document.payments
WHERE contract_id IN (SELECT new_id FROM migration.contract_id_map);

\if :apply
\echo '>>> APPLY: x100'
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM migration.x100_contracts_done) THEN
    RAISE NOTICE 'x100 allaqachon qo''llangan — o''tkazib yuborildi';
  ELSE
    UPDATE document.contracts
       SET price = price * 100,
           initial_payment = initial_payment * 100,
           last_modified_date = now()
     WHERE id IN (SELECT new_id FROM migration.contract_id_map);

    UPDATE document.payments
       SET total_amount = total_amount * 100,
           paid_amount  = coalesce(paid_amount, 0) * 100,
           updated_at   = now()
     WHERE contract_id IN (SELECT new_id FROM migration.contract_id_map);

    INSERT INTO migration.x100_contracts_done DEFAULT VALUES;
  END IF;
END $$;

\echo '=== APPLY natija ==='
SELECT count(*) AS cnt, min(price) AS min_price, max(price) AS max_price
FROM document.contracts
WHERE id IN (SELECT new_id FROM migration.contract_id_map) AND price IS NOT NULL;
SELECT count(*) AS cnt, min(total_amount) AS min_total, max(total_amount) AS max_total
FROM document.payments
WHERE contract_id IN (SELECT new_id FROM migration.contract_id_map);
\endif

COMMIT;
