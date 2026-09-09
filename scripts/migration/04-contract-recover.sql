-- Item 3: oldingi staging'dan tushib qolgan shartnomalarni tiklash (asosiy maydonlar,
-- oldingi migratsiya uslubida: document_json bo'sh, template_id type_id'dan).
-- :apply=true bo'lsa yozadi.
\set ON_ERROR_STOP on
BEGIN;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

\echo '=== EMPIRIK: eski status -> yangi status (ko-chgan contractlardan) ==='
SELECT c3.status AS old_status, dc.status AS new_status, count(*)
FROM migration.stage_c3u c3 JOIN migration.contract_id_map m ON m.old_id=c3.id
JOIN document.contracts dc ON dc.id=m.new_id GROUP BY 1,2 ORDER BY 3 DESC;

\echo '=== number = old id? ==='
SELECT count(*) FILTER (WHERE dc.number=c3.id::text) AS num_eq_oldid, count(*) total
FROM migration.stage_c3u c3 JOIN migration.contract_id_map m ON m.old_id=c3.id JOIN document.contracts dc ON dc.id=m.new_id;

-- Tiklash to'plami: ko'chmagan, ikki pnfl bor, type in (1,2,3), o'chirilmagan
DROP TABLE IF EXISTS recov;
CREATE TEMP TABLE recov AS
SELECT c.id AS old_id,
  uuid_generate_v5(uuid_ns_url(),'hesap:old-contract:'||c.id) AS new_id,
  (CASE c.type_id WHEN '1' THEN 'f81e5e97-d7c5-4621-a53e-cc3593b4b086'
                  WHEN '2' THEN '09b1d06c-d2ef-463e-ae4f-ced54686425e'
                  WHEN '3' THEN 'bbecd51f-6bb4-47ba-8a78-257b91d0b08a' END)::uuid AS template_id,
  ((SELECT COALESCE(MAX(NULLIF(regexp_replace(number,'[^0-9]','','g'),'')::bigint),0) FROM document.contracts)
    + row_number() OVER (ORDER BY c.id))::text AS number,
  (CASE upper(c.status) WHEN 'ACTIVE' THEN 'ACTIVE' WHEN 'CLOSE' THEN 'COMPLETED'
                        WHEN 'REJECTED' THEN 'REJECTED' WHEN 'CANCELLED' THEN 'CANCELLED'
                        ELSE 'COMPLETED' END) AS status,
  c.debtor_pnfl AS buyer_in, c.lender_pnfl AS seller_in,
  cu.pnfl AS creator_in,
  nullif(c.amount,'')::numeric AS price,
  (CASE c.currency_id WHEN '1' THEN 'UZS' WHEN '2' THEN 'RUB' WHEN '3' THEN 'USD' ELSE 'UZS' END) AS currency,
  (CASE c.currency_id WHEN '1' THEN 'b6901715-f22f-44da-803c-fecc45e5b1f6'
                      WHEN '2' THEN 'ed2b00b3-75e0-40dd-bd1b-59a00cd97fa2'
                      WHEN '3' THEN '0a816838-8151-4859-8b51-f7269f2c23ae'
                      ELSE 'b6901715-f22f-44da-803c-fecc45e5b1f6' END)::uuid AS currency_id
FROM migration.stage_c3u c
LEFT JOIN migration.old_users cu ON cu.id = nullif(c.creator_id,'')::bigint
WHERE NOT EXISTS(SELECT 1 FROM migration.contract_id_map m WHERE m.old_id=c.id)
  AND c.lender_pnfl<>'' AND c.debtor_pnfl<>'' AND c.type_id IN ('1','2','3')
  AND coalesce(c.del,'false')='false';

\echo '=== tiklash to-plami soni ==='
SELECT count(*) FROM recov;
\echo '=== status taqsimoti ==='
SELECT status,count(*) FROM recov GROUP BY 1;
\echo '=== namuna ==='
SELECT old_id,number,status,seller_in,buyer_in,creator_in,price,currency FROM recov ORDER BY old_id LIMIT 10;

\echo '=== number collision: recov.number allaqachon document.contracts da bormi? ==='
SELECT count(*) FROM recov r WHERE EXISTS(SELECT 1 FROM document.contracts c WHERE c.number=r.number);
\echo '=== number ustunida unique constraint bormi? ==='
SELECT conname FROM pg_constraint WHERE conrelid='document.contracts'::regclass AND contype='u';
SELECT indexname FROM pg_indexes WHERE schemaname='document' AND tablename='contracts' AND indexdef ILIKE '%unique%';

\if :apply
\echo '>>> APPLY: document.contracts'
INSERT INTO document.contracts
  (id, template_id, number, status, price, deleted, created_date, last_modified_date, version,
   buyer_status, seller_status, type, document_json, currency, initial_payment, delivery_at,
   buyer_in, seller_in, currency_id, creator_in)
SELECT r.new_id, r.template_id, r.number, r.status, r.price, false, now(), now(), 0,
   -- DocumentPartyStatus enum: PENDING|ACCEPTED|REJECTED|CANCELLED. Imzolangan = ACCEPTED
   -- ('SIGNED' enumda yo'q → valueOf() 500 beradi, "Faol" tab ochilmaydi).
   CASE WHEN r.status='REJECTED' THEN 'REJECTED' ELSE 'ACCEPTED' END,
   CASE WHEN r.status='REJECTED' THEN 'REJECTED' ELSE 'ACCEPTED' END,
   'C2C', '', r.currency, NULL, NULL, r.buyer_in, r.seller_in, r.currency_id, r.creator_in
FROM recov r
ON CONFLICT (id) DO NOTHING;
INSERT INTO migration.contract_id_map (old_id, new_id, migrated_at)
SELECT r.old_id, r.new_id, now() FROM recov r ON CONFLICT (old_id) DO NOTHING;
\echo '=== natija: document.contracts jami ==='
SELECT count(*) FROM document.contracts;
\endif
COMMIT;
