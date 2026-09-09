-- Item 3 bolalar: tiklangan 165 shartnoma uchun payments + witnesses (eski payment/witness dan).
-- Tiklangan to'plam = contract_id_map da new_id deterministik uuid (hesap:old-contract:<id>) bo'lganlar.
\set ON_ERROR_STOP on
BEGIN;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE TABLE IF NOT EXISTS migration.stage_payment (id bigint, contract_id bigint, lender_pnfl text, debtor_pnfl text, price text, paid text, is_paid text, status text, currency_id text, arrangement_at text, paid_at text, created_at text);
CREATE TABLE IF NOT EXISTS migration.stage_witness (id bigint, contract_id bigint, user_id bigint, status text, created_at text);
TRUNCATE migration.stage_payment, migration.stage_witness;
\copy migration.stage_payment FROM '/mig/payment.csv' WITH (FORMAT csv, HEADER true)
\copy migration.stage_witness FROM '/mig/witness.csv' WITH (FORMAT csv, HEADER true)
-- CSV boolean t/f -> true/false (is_paid tekshiruvi 'true' matnini kutadi)
UPDATE migration.stage_payment SET is_paid = CASE is_paid WHEN 't' THEN 'true' WHEN 'f' THEN 'false' ELSE is_paid END;

DROP TABLE IF EXISTS recov;
CREATE TEMP TABLE recov AS
SELECT m.old_id, m.new_id FROM migration.contract_id_map m
WHERE m.new_id = uuid_generate_v5(uuid_ns_url(),'hesap:old-contract:'||m.old_id);
\echo '=== tiklangan contract soni ==='
SELECT count(*) FROM recov;
\echo '=== ko-chiriladigan payments ==='
SELECT count(*) FROM migration.stage_payment p JOIN recov r ON r.old_id=p.contract_id;
\echo '=== ko-chiriladigan witnesses (status=1, user_id_map bor) ==='
SELECT count(*) FROM migration.stage_witness w JOIN recov r ON r.old_id=w.contract_id
 JOIN migration.user_id_map um ON um.old_id=w.user_id WHERE w.status='1';

\if :apply
\echo '>>> APPLY payments'
INSERT INTO document.payments
 (id, contract_id, buyer_in, seller_in, status, contract_status, total_amount, paid_amount, currency, currency_id, contract_payment_date, changed_payment_date, paid_at, deleted, created_at, updated_at)
SELECT uuid_generate_v5(uuid_ns_url(),'hesap:old-payment:'||p.id),
  r.new_id, dc.buyer_in, dc.seller_in,
  CASE WHEN lower(coalesce(p.is_paid,''))='true' OR upper(coalesce(p.status,''))='FULL' THEN 'PAID' ELSE 'PENDING' END,
  dc.status, nullif(p.price,'')::numeric, coalesce(nullif(p.paid,'')::numeric,0),
  dc.currency, dc.currency_id,
  nullif(p.arrangement_at,'')::timestamp, NULL, nullif(p.paid_at,'')::timestamp,
  false, coalesce(nullif(p.created_at,'')::timestamp, now()), now()
FROM migration.stage_payment p JOIN recov r ON r.old_id=p.contract_id
JOIN document.contracts dc ON dc.id=r.new_id
ON CONFLICT (id) DO NOTHING;

\echo '>>> APPLY witnesses'
INSERT INTO document.witnesses (id, contract_id, witness_id, created_date, last_modified_date)
SELECT uuid_generate_v5(uuid_ns_url(),'hesap:old-witness:'||w.id), r.new_id, um.new_id,
  coalesce(nullif(w.created_at,'')::timestamp, now()), now()
FROM migration.stage_witness w JOIN recov r ON r.old_id=w.contract_id
JOIN migration.user_id_map um ON um.old_id=w.user_id
WHERE w.status='1'
ON CONFLICT (id) DO NOTHING;

\echo '=== natija ==='
SELECT 'recovered payments='||count(*) FROM document.payments p JOIN recov r ON r.new_id=p.contract_id;
SELECT 'recovered witnesses='||count(*) FROM document.witnesses w JOIN recov r ON r.new_id=w.contract_id;
\endif
COMMIT;
