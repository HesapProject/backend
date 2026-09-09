-- Qolgan legacy ma'lumotlar: passport -> integration.myid_profile,
-- notification -> integration.notification, contract_notice -> document.notices,
-- contract_reports -> document.claims, payment_requests -> document.payment_requests
-- + document.delay_requests, user_package_usage -> "user".user_package_usage.
-- Idempotent: deterministik uuid_v5 + ON CONFLICT DO NOTHING. :apply=false -> PREVIEW.
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE SCHEMA IF NOT EXISTS migration;

DROP TABLE IF EXISTS migration.stage_passport;
DROP TABLE IF EXISTS migration.stage_notification;
DROP TABLE IF EXISTS migration.stage_notice;
DROP TABLE IF EXISTS migration.stage_report;
DROP TABLE IF EXISTS migration.stage_payreq;
DROP TABLE IF EXISTS migration.stage_upu;

CREATE TABLE migration.stage_passport (id bigint, user_id bigint, pnfl text, number text,
  first_name text, middle_name text, last_name text, birth_date text, birth_place text,
  give_place text, start_date text, end_date text, is_man text, status text, active text,
  is_deleted text, created_at text, updated_at text);
CREATE TABLE migration.stage_notification (id bigint, data_id bigint, type int, user_id bigint,
  title_uz text, title_ru text, title_en text, body_uz text, body_ru text, body_en text,
  image text, created_at text, is_deleted text);
CREATE TABLE migration.stage_notice (id bigint, contract_id bigint, number int,
  lender_id bigint, debtor_id bigint, status text, name_uz text, name_ru text, name_en text,
  created_at text, updated_at text);
CREATE TABLE migration.stage_report (id bigint, contract_id bigint, lender_id bigint,
  debtor_id bigint, status text, created_at text, updated_at text);
CREATE TABLE migration.stage_payreq (id bigint, payment_id bigint, amount text, status text,
  created_at text, updated_at text, accepted_at text, image text, comment text,
  lender_id bigint, debtor_id bigint, date_change_from text, date_change_to text,
  is_payment text, is_date_change text, contract_id bigint, is_active text);
CREATE TABLE migration.stage_upu (id bigint, user_id bigint, contract_id bigint,
  type_id bigint, created_at text);

\copy migration.stage_passport     FROM '/mig/passport.csv'      WITH (FORMAT csv, HEADER true)
\copy migration.stage_notification FROM '/mig/notification.csv'  WITH (FORMAT csv, HEADER true)
\copy migration.stage_notice       FROM '/mig/notice.csv'        WITH (FORMAT csv, HEADER true)
\copy migration.stage_report       FROM '/mig/report.csv'        WITH (FORMAT csv, HEADER true)
\copy migration.stage_payreq       FROM '/mig/payreq.csv'        WITH (FORMAT csv, HEADER true)
\copy migration.stage_upu          FROM '/mig/upu.csv'           WITH (FORMAT csv, HEADER true)

\echo '=== STAGE countlar ==='
SELECT 'passport' t, count(*) FROM migration.stage_passport
UNION ALL SELECT 'notification', count(*) FROM migration.stage_notification
UNION ALL SELECT 'notice', count(*) FROM migration.stage_notice
UNION ALL SELECT 'report', count(*) FROM migration.stage_report
UNION ALL SELECT 'payreq', count(*) FROM migration.stage_payreq
UNION ALL SELECT 'upu', count(*) FROM migration.stage_upu;

\echo '=== payreq status taqsimoti (mapping tekshiruvi) ==='
SELECT status, is_payment, is_date_change, count(*) FROM migration.stage_payreq
GROUP BY 1,2,3 ORDER BY 4 DESC LIMIT 15;

\echo '=== mavjud integration.notification type qiymatlari ==='
SELECT type, count(*) FROM integration.notification GROUP BY 1 ORDER BY 2 DESC LIMIT 15;

\echo '=== integration.myid_profile amaldagi ustunlar ==='
SELECT string_agg(column_name, ', ' ORDER BY ordinal_position)
FROM information_schema.columns
WHERE table_schema='integration' AND table_name='myid_profile';

\echo '=== diag: migration yordamchi jadvallar ustunlari ==='
SELECT 'legacy_payment: '||string_agg(column_name, ', ' ORDER BY ordinal_position)
FROM information_schema.columns WHERE table_schema='migration' AND table_name='legacy_payment';
SELECT 'prepare_map: '||string_agg(column_name, ', ' ORDER BY ordinal_position)
FROM information_schema.columns WHERE table_schema='migration' AND table_name='prepare_map';
SELECT 'contract_resolved: '||string_agg(column_name, ', ' ORDER BY ordinal_position)
FROM information_schema.columns WHERE table_schema='migration' AND table_name='contract_resolved';

\echo '=== diag: payreq.payment_id qaysi domenda? ==='
SELECT 'payreq->legacy_payment(id)' t, count(*) FROM migration.stage_payreq s
  WHERE EXISTS (SELECT 1 FROM migration.legacy_payment lp WHERE lp.id::text=s.payment_id::text);
SELECT 'notice->prepare_map(prepare_id)' t, count(*) FROM migration.stage_notice s
  WHERE EXISTS (SELECT 1 FROM migration.prepare_map pm WHERE pm.prepare_id::text=s.contract_id::text);
SELECT 'report->prepare_map(prepare_id)' t, count(*) FROM migration.stage_report s
  WHERE EXISTS (SELECT 1 FROM migration.prepare_map pm WHERE pm.prepare_id::text=s.contract_id::text);

-- Eski to'lov id -> yangi document.payments id: contract + sana (+ summa) bo'yicha.
DROP TABLE IF EXISTS migration.pay_id_map;
CREATE TABLE migration.pay_id_map AS
SELECT lp.id AS old_payment_id, px.id AS new_payment_id
FROM migration.legacy_payment lp
JOIN LATERAL (
  SELECT cm.new_id FROM migration.contract_id_map cm WHERE cm.old_id::text=lp.contract_id::text
  UNION ALL
  SELECT cm2.new_id FROM migration.prepare_map pm
  JOIN migration.contract_id_map cm2 ON cm2.old_id::text=pm.contract_id::text
  WHERE pm.prepare_id::text=lp.contract_id::text
  LIMIT 1
) cmap ON true
JOIN LATERAL (
  SELECT p.id FROM document.payments p
  WHERE p.contract_id=cmap.new_id
    AND date_trunc('day', p.contract_payment_date) = date_trunc('day', nullif(lp.arrangement_at,'')::timestamp)
  ORDER BY (abs(coalesce(p.total_amount,0) - coalesce(nullif(lp.price,'')::numeric,0)*100)) ASC
  LIMIT 1
) px ON true;
-- 2-bosqich: kun mos kelmagan legacy to'lovlar uchun shu contract ichida ENG YAQIN sanali to'lov.
INSERT INTO migration.pay_id_map
SELECT lp.id, px.id
FROM migration.legacy_payment lp
JOIN LATERAL (
  SELECT cm.new_id FROM migration.contract_id_map cm WHERE cm.old_id::text=lp.contract_id::text
  UNION ALL
  SELECT cm2.new_id FROM migration.prepare_map pm
  JOIN migration.contract_id_map cm2 ON cm2.old_id::text=pm.contract_id::text
  WHERE pm.prepare_id::text=lp.contract_id::text
  LIMIT 1
) cmap ON true
JOIN LATERAL (
  SELECT p.id FROM document.payments p
  WHERE p.contract_id=cmap.new_id
  ORDER BY abs(extract(epoch FROM (p.contract_payment_date - nullif(lp.arrangement_at,'')::timestamp))) ASC,
           abs(coalesce(p.total_amount,0) - coalesce(nullif(lp.price,'')::numeric,0)*100) ASC
  LIMIT 1
) px ON true
WHERE NOT EXISTS (SELECT 1 FROM migration.pay_id_map e WHERE e.old_payment_id=lp.id);
SELECT 'pay_id_map' t, count(*) FROM migration.pay_id_map;

\echo '=== diag: payreq -> contract map bormi? ==='
SELECT 'payreq lp bilan' t, count(*) FROM migration.stage_payreq s
  JOIN migration.legacy_payment lp ON lp.id::text=s.payment_id::text;
SELECT 'payreq lp+contract map' t, count(*) FROM migration.stage_payreq s
  JOIN migration.legacy_payment lp ON lp.id::text=s.payment_id::text
  WHERE EXISTS (SELECT 1 FROM migration.contract_id_map cm WHERE cm.old_id::text=lp.contract_id::text)
     OR EXISTS (SELECT 1 FROM migration.prepare_map pm
                JOIN migration.contract_id_map cm2 ON cm2.old_id::text=pm.contract_id::text
                WHERE pm.prepare_id::text=lp.contract_id::text);

\echo '=== ko-chiriladigan (map bor) countlar ==='
SELECT 'passport->myid_profile' t, count(*) FROM (
  SELECT DISTINCT ON (u.pinfl) s.id FROM migration.stage_passport s
  JOIN migration.user_id_map m ON m.old_id=s.user_id
  JOIN "user"."user" u ON u.id=m.new_id
  WHERE coalesce(s.is_deleted,'f') NOT IN ('t','true') AND u.pinfl IS NOT NULL
    AND NOT EXISTS (SELECT 1 FROM integration.myid_profile mp WHERE mp.user_in=u.pinfl)
  ORDER BY u.pinfl, (s.active IN ('t','true')) DESC, s.id DESC) x
UNION ALL SELECT 'notification', count(*) FROM migration.stage_notification s
  JOIN migration.user_id_map m ON m.old_id=s.user_id
UNION ALL SELECT 'notice', count(*) FROM migration.stage_notice s
  WHERE EXISTS (SELECT 1 FROM migration.contract_id_map cm WHERE cm.old_id=s.contract_id)
     OR EXISTS (SELECT 1 FROM migration.prepare_map pm
                JOIN migration.contract_id_map cm2 ON cm2.old_id::text=pm.contract_id::text
                WHERE pm.prepare_id::text=s.contract_id::text)
UNION ALL SELECT 'report', count(*) FROM migration.stage_report s
  WHERE EXISTS (SELECT 1 FROM migration.contract_id_map cm WHERE cm.old_id=s.contract_id)
     OR EXISTS (SELECT 1 FROM migration.prepare_map pm
                JOIN migration.contract_id_map cm2 ON cm2.old_id::text=pm.contract_id::text
                WHERE pm.prepare_id::text=s.contract_id::text)
UNION ALL SELECT 'payreq-payment', count(*) FROM migration.stage_payreq s
  WHERE s.is_payment IN ('t','true')
    AND EXISTS (SELECT 1 FROM migration.pay_id_map pmap WHERE pmap.old_payment_id::text=s.payment_id::text)
UNION ALL SELECT 'payreq-delay', count(*) FROM migration.stage_payreq s
  WHERE s.is_date_change IN ('t','true')
    AND EXISTS (SELECT 1 FROM migration.pay_id_map pmap WHERE pmap.old_payment_id::text=s.payment_id::text)
UNION ALL SELECT 'upu', count(*) FROM migration.stage_upu s
  JOIN migration.user_id_map m ON m.old_id=s.user_id;

\if :apply

\echo '>>> APPLY: passport -> integration.myid_profile'
INSERT INTO integration.myid_profile
  (id, user_in, first_name, middle_name, last_name, pinfl, gender, birth_place,
   birth_date, pass_data, issued_by, issued_date, expiry_date, created_date, last_modified_date)
SELECT uuid_generate_v5(uuid_ns_url(),'hesap:old-passport:'||x.id), x.pinfl_u,
       x.first_name, x.middle_name, x.last_name, nullif(x.pnfl,''),
       CASE x.is_man WHEN 'true' THEN '1' WHEN 'false' THEN '2' END,
       nullif(x.birth_place,''), nullif(x.birth_date,''), nullif(x.number,''),
       nullif(x.give_place,''), nullif(x.start_date,''), nullif(x.end_date,''),
       coalesce(nullif(x.created_at,'')::timestamp, now()),
       coalesce(nullif(x.updated_at,'')::timestamp, nullif(x.created_at,'')::timestamp, now())
FROM (
  SELECT DISTINCT ON (u.pinfl) s.*, u.pinfl AS pinfl_u FROM migration.stage_passport s
  JOIN migration.user_id_map m ON m.old_id=s.user_id
  JOIN "user"."user" u ON u.id=m.new_id
  WHERE coalesce(s.is_deleted,'f') NOT IN ('t','true') AND u.pinfl IS NOT NULL
    AND NOT EXISTS (SELECT 1 FROM integration.myid_profile mp WHERE mp.user_in=u.pinfl)
  ORDER BY u.pinfl, (s.active IN ('t','true')) DESC, s.id DESC) x
ON CONFLICT (id) DO NOTHING;

\echo '>>> APPLY: notification -> integration.notification'
INSERT INTO integration.notification
  (id, data_id, type, user_id, title_uz, title_ru, title_en, body_uz, body_ru, body_en,
   image, is_viewed, deleted, created_at, updated_at)
SELECT uuid_generate_v5(uuid_ns_url(),'hesap:old-notification:'||s.id),
       cm.new_id,
       CASE s.type WHEN 1 THEN 'NEWS' WHEN 2 THEN 'CONTRACT' WHEN 3 THEN 'WITNESS'
         WHEN 10 THEN 'PERMISSION_REQUEST' WHEN 11 THEN 'PERMISSION_ACCEPTED'
         WHEN 12 THEN 'PERMISSION_CANCELLED' WHEN 13 THEN 'PERMISSION_REJECTED'
         WHEN 21 THEN 'PASSPORT_ACCEPTED' WHEN 22 THEN 'PASSPORT_CONFLICTED'
         WHEN 23 THEN 'PASSPORT_EXPIRED' WHEN 24 THEN 'PASSPORT_REJECTED'
         WHEN 30 THEN 'PAYMENT_REQUEST' WHEN 31 THEN 'PAYMENT_ACCEPT' WHEN 32 THEN 'PAYMENT_REJECT'
         WHEN 33 THEN 'PAYMENT_PAID' WHEN 34 THEN 'PAYMENT_PAID_APPROVED' WHEN 35 THEN 'PAYMENT_PAID_REJECTED'
         WHEN 40 THEN 'PAYMENT_DELAY_REQUEST' WHEN 41 THEN 'PAYMENT_DELAY_APPROVED' WHEN 42 THEN 'PAYMENT_DELAY_REJECTED'
         WHEN 50 THEN 'WITNESS_ACCEPTED' WHEN 51 THEN 'WITNESS_REJECTED' WHEN 56 THEN 'WITNESS_INVITED'
         WHEN 52 THEN 'DOCUMENT_SIGNED' WHEN 53 THEN 'DOCUMENT_REJECTED' WHEN 54 THEN 'DOCUMENT_CANCELLED'
         WHEN 55 THEN 'DOCUMENT_COMPLETED' WHEN 57 THEN 'DOCUMENT_CREATED'
         WHEN 60 THEN 'REPORT_CREATED' WHEN 61 THEN 'NOTICE_CREATED'
         ELSE 'CONTRACT' END,
       um.new_id, s.title_uz, s.title_ru, s.title_en, s.body_uz, s.body_ru, s.body_en,
       nullif(s.image,''), TRUE, s.is_deleted IN ('t','true'),
       coalesce(nullif(s.created_at,'')::timestamp, now()),
       coalesce(nullif(s.created_at,'')::timestamp, now())
FROM migration.stage_notification s
JOIN migration.user_id_map um ON um.old_id=s.user_id
LEFT JOIN migration.contract_id_map cm ON cm.old_id=s.data_id AND s.type NOT IN (1,21,22,23,24)
ON CONFLICT (id) DO NOTHING;

\echo '>>> APPLY: contract_notice -> document.notices'
INSERT INTO document.notices
  (id, contract_id, name_uz, name_ru, name_en, number, status, deleted,
   created_date, last_modified_date, buyer_in, seller_in)
SELECT uuid_generate_v5(uuid_ns_url(),'hesap:old-notice:'||s.id), c.id,
       s.name_uz, s.name_ru, s.name_en, s.number, coalesce(nullif(s.status,''),'CREATED'), FALSE,
       coalesce(nullif(s.created_at,'')::timestamp, now()),
       coalesce(nullif(s.updated_at,'')::timestamp, nullif(s.created_at,'')::timestamp, now()),
       c.buyer_in, c.seller_in
FROM migration.stage_notice s
JOIN LATERAL (
  SELECT cm.new_id FROM migration.contract_id_map cm WHERE cm.old_id=s.contract_id
  UNION ALL
  SELECT cm2.new_id FROM migration.prepare_map pm
  JOIN migration.contract_id_map cm2 ON cm2.old_id::text=pm.contract_id::text
  WHERE pm.prepare_id::text=s.contract_id::text
  LIMIT 1
) mapx ON true
JOIN document.contracts c ON c.id=mapx.new_id
ON CONFLICT (id) DO NOTHING;

\echo '>>> APPLY: contract_reports -> document.claims'
INSERT INTO document.claims
  (id, contract_id, status, deleted, created_date, last_modified_date, from_in, to_in)
SELECT uuid_generate_v5(uuid_ns_url(),'hesap:old-claim:'||s.id), cm.new_id,
       coalesce(nullif(s.status,''),'CREATED'), FALSE,
       coalesce(nullif(s.created_at,'')::timestamp, now()),
       coalesce(nullif(s.updated_at,'')::timestamp, nullif(s.created_at,'')::timestamp, now()),
       lu.pinfl, du.pinfl
FROM migration.stage_report s
JOIN LATERAL (
  SELECT cm.new_id FROM migration.contract_id_map cm WHERE cm.old_id=s.contract_id
  UNION ALL
  SELECT cm2.new_id FROM migration.prepare_map pm
  JOIN migration.contract_id_map cm2 ON cm2.old_id::text=pm.contract_id::text
  WHERE pm.prepare_id::text=s.contract_id::text
  LIMIT 1
) cm ON true
LEFT JOIN migration.user_id_map lm ON lm.old_id=s.lender_id
LEFT JOIN "user"."user" lu ON lu.id=lm.new_id
LEFT JOIN migration.user_id_map dm ON dm.old_id=s.debtor_id
LEFT JOIN "user"."user" du ON du.id=dm.new_id
ON CONFLICT (id) DO NOTHING;

\echo '>>> APPLY: payment_requests (to-lov) -> document.payment_requests'
INSERT INTO document.payment_requests
  (id, contract_id, payment_date, status, amount, deleted, created_date, last_modified_date,
   payment_id, currency, buyer_in, seller_in, note, image)
SELECT uuid_generate_v5(uuid_ns_url(),'hesap:old-payreq:'||s.id),
       p.contract_id, p.contract_payment_date,
       CASE upper(coalesce(s.status,'')) WHEN 'CREATED' THEN 'PENDING'
         WHEN 'ACCEPTED' THEN 'APPROVED' WHEN 'ACCEPT' THEN 'APPROVED'
         WHEN 'REJECTED' THEN 'REJECTED' WHEN 'REJECT' THEN 'REJECTED'
         WHEN 'CANCELLED' THEN 'CANCELLED' ELSE upper(coalesce(s.status,'PENDING')) END,
       nullif(s.amount,'')::numeric * 100, FALSE,
       coalesce(nullif(s.created_at,'')::timestamp, now()),
       coalesce(nullif(s.updated_at,'')::timestamp, nullif(s.created_at,'')::timestamp, now()),
       p.id, coalesce(p.currency,'UZS'), p.buyer_in, p.seller_in,
       nullif(s.comment,''), nullif(s.image,'')
FROM migration.stage_payreq s
JOIN migration.pay_id_map pmap ON pmap.old_payment_id::text=s.payment_id::text
JOIN document.payments p ON p.id=pmap.new_payment_id
WHERE s.is_payment IN ('t','true')
ON CONFLICT (id) DO NOTHING;

\echo '>>> APPLY: payment_requests (kechiktirish) -> document.delay_requests'
INSERT INTO document.delay_requests
  (id, contract_id, payment_date, status, amount, deleted, created_date, last_modified_date,
   payment_id, currency, buyer_in, seller_in, note)
SELECT uuid_generate_v5(uuid_ns_url(),'hesap:old-delayreq:'||s.id),
       p.contract_id, coalesce(nullif(s.date_change_to,'')::timestamp, p.contract_payment_date),
       CASE upper(coalesce(s.status,'')) WHEN 'CREATED' THEN 'PENDING'
         WHEN 'ACCEPTED' THEN 'APPROVED' WHEN 'ACCEPT' THEN 'APPROVED'
         WHEN 'REJECTED' THEN 'REJECTED' WHEN 'REJECT' THEN 'REJECTED'
         WHEN 'CANCELLED' THEN 'CANCELLED' ELSE upper(coalesce(s.status,'PENDING')) END,
       nullif(s.amount,'')::numeric * 100, FALSE,
       coalesce(nullif(s.created_at,'')::timestamp, now()),
       coalesce(nullif(s.updated_at,'')::timestamp, nullif(s.created_at,'')::timestamp, now()),
       p.id, coalesce(p.currency,'UZS'), p.buyer_in, p.seller_in, nullif(s.comment,'')
FROM migration.stage_payreq s
JOIN migration.pay_id_map pmap ON pmap.old_payment_id::text=s.payment_id::text
JOIN document.payments p ON p.id=pmap.new_payment_id
WHERE s.is_date_change IN ('t','true')
ON CONFLICT (id) DO NOTHING;

\echo '>>> APPLY: user_package_usage -> "user".user_package_usage'
INSERT INTO "user".user_package_usage
  (id, user_in, user_package_id, template_id, contract_id, status, created_date)
SELECT uuid_generate_v5(uuid_ns_url(),'hesap:old-upu:'||s.id),
       u.pinfl, NULL, tm.new_id, cm.new_id, 'USED',
       coalesce(nullif(s.created_at,'')::timestamp, now())
FROM migration.stage_upu s
JOIN migration.user_id_map m ON m.old_id=s.user_id
JOIN "user"."user" u ON u.id=m.new_id
LEFT JOIN migration.contract_template_id_map tm ON tm.old_id::text=s.type_id::text
LEFT JOIN migration.contract_id_map cm ON cm.old_id=s.contract_id
ON CONFLICT (id) DO NOTHING;

\echo '=== YAKUN: yangi countlar ==='
SELECT 'integration.myid_profile' t, count(*) FROM integration.myid_profile
UNION ALL SELECT 'integration.notification', count(*) FROM integration.notification
UNION ALL SELECT 'document.notices', count(*) FROM document.notices
UNION ALL SELECT 'document.claims', count(*) FROM document.claims
UNION ALL SELECT 'document.payment_requests', count(*) FROM document.payment_requests
UNION ALL SELECT 'document.delay_requests', count(*) FROM document.delay_requests
UNION ALL SELECT 'user.user_package_usage', count(*) FROM "user".user_package_usage;

\else
\echo 'PREVIEW rejimi — yozilmadi. Yozish uchun commit xabariga [apply] qo-shing.'
\endif
