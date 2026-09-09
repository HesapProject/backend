-- Item 2 (permissions→white_list, white_list_request) + Item 4 (packages, user_package)
-- Eski trust'dan CSV staging → transform. Idempotent (deterministik uuid_v5 + ON CONFLICT).
-- :apply = 'true' bo'lsa INSERT qiladi; aks holda faqat PREVIEW (yozmaydi).
\set ON_ERROR_STOP on
BEGIN;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE SCHEMA IF NOT EXISTS migration;

-- ---------- STAGING ----------
CREATE TABLE IF NOT EXISTS migration.stage_permission (id bigint, user_f bigint, user_t bigint, passport boolean, partner boolean, contract boolean, payability boolean, created_at timestamp, updated_at timestamp, active boolean);
CREATE TABLE IF NOT EXISTS migration.stage_permreq   (id bigint, user_f bigint, user_t bigint, contract boolean, payability boolean, passport boolean, partner boolean, status text, created_at timestamp, updated_at timestamp, is_deleted boolean);
CREATE TABLE IF NOT EXISTS migration.stage_docpkg    (id integer, name_uz text, name_ru text, name_en text, price double precision, document_count integer, expired_date timestamp, created_date timestamp, last_modified_date timestamp, deleted boolean, type text, document_type_id integer, description_uz text, description_ru text, description_en text);
CREATE TABLE IF NOT EXISTS migration.stage_userpkg   (id integer, user_id bigint, package_id bigint, price_packet bigint, document_count integer, expired_date timestamp, updated_at timestamp, created_at timestamp, promo_id integer, promo_code text, additional_document_count integer, transaction_id text);
TRUNCATE migration.stage_permission, migration.stage_permreq, migration.stage_docpkg, migration.stage_userpkg;

\copy migration.stage_permission FROM '/mig/permission.csv'    WITH (FORMAT csv, HEADER true)
\copy migration.stage_permreq    FROM '/mig/permission_req.csv' WITH (FORMAT csv, HEADER true)
\copy migration.stage_docpkg     FROM '/mig/document_package.csv' WITH (FORMAT csv, HEADER true)
\copy migration.stage_userpkg    FROM '/mig/user_package.csv'   WITH (FORMAT csv, HEADER true)

-- ---------- PREVIEW (doim) ----------
\echo '=== staging satrlar ==='
SELECT 'permission='||count(*) FROM migration.stage_permission
UNION ALL SELECT 'permreq='||count(*) FROM migration.stage_permreq
UNION ALL SELECT 'docpkg='||count(*) FROM migration.stage_docpkg
UNION ALL SELECT 'userpkg='||count(*) FROM migration.stage_userpkg;

\echo '=== permreq status qiymatlari (enum map uchun) ==='
SELECT coalesce(status,'NULL')||': '||count(*) FROM migration.stage_permreq GROUP BY status;

\echo '=== white_list: ko-chiriladigan (ikkala pnfl bor) ==='
SELECT count(*) FROM migration.stage_permission sp
JOIN migration.old_users uf ON uf.id=sp.user_f
JOIN migration.old_users ut ON ut.id=sp.user_t
WHERE uf.pnfl IS NOT NULL AND uf.pnfl<>'' AND ut.pnfl IS NOT NULL AND ut.pnfl<>'';

\echo '=== white_list_request: ko-chiriladigan (ikkala user_id_map bor) ==='
SELECT count(*) FROM migration.stage_permreq sr
JOIN migration.user_id_map mf ON mf.old_id=sr.user_f
JOIN migration.user_id_map mt ON mt.old_id=sr.user_t;

\echo '=== user_package: ko-chiriladigan (user pnfl bor + paket document_package-da) ==='
SELECT count(*) FROM migration.stage_userpkg up
JOIN migration.old_users u ON u.id=up.user_id
JOIN migration.stage_docpkg dp ON dp.id=up.package_id
WHERE u.pnfl IS NOT NULL AND u.pnfl<>'';

\echo '=== maqsad jadval ustunlari (tekshirish) ==='
SELECT table_name||': '||string_agg(column_name,',' ORDER BY ordinal_position)
FROM information_schema.columns
WHERE table_schema='user' AND table_name IN ('white_list','white_list_request','packages','user_package')
GROUP BY table_name ORDER BY table_name;

-- ---------- APPLY (faqat :apply=true) ----------
\if :apply
\echo '>>> APPLY: white_list'
INSERT INTO "user".white_list (id, user_from_in, user_to_in, user_info, passport, payability, contract, partner, active, deleted, created_date, last_modified_date)
SELECT uuid_generate_v5(uuid_ns_url(), 'hesap:old-perm:'||sp.id),
       uf.pnfl, ut.pnfl, false,
       coalesce(sp.passport,false), coalesce(sp.payability,false), coalesce(sp.contract,false), coalesce(sp.partner,false),
       coalesce(sp.active,true), false, coalesce(sp.created_at, now()), coalesce(sp.updated_at, sp.created_at, now())
FROM migration.stage_permission sp
JOIN migration.old_users uf ON uf.id=sp.user_f
JOIN migration.old_users ut ON ut.id=sp.user_t
WHERE uf.pnfl IS NOT NULL AND uf.pnfl<>'' AND ut.pnfl IS NOT NULL AND ut.pnfl<>''
ON CONFLICT (id) DO NOTHING;

\echo '>>> APPLY: white_list_request'
INSERT INTO "user".white_list_request (id, user_from_id, user_to_id, user_info, passport, payability, contract, partner, status, deleted, created_date, last_modified_date)
SELECT uuid_generate_v5(uuid_ns_url(), 'hesap:old-permreq:'||sr.id),
       mf.new_id, mt.new_id, false,
       coalesce(sr.passport,false), coalesce(sr.payability,false), coalesce(sr.contract,false), coalesce(sr.partner,false),
       CASE upper(coalesce(sr.status,'PENDING'))
         WHEN 'ACCEPTED' THEN 'APPROVED' WHEN 'APPROVE' THEN 'APPROVED' WHEN 'APPROVED' THEN 'APPROVED'
         WHEN 'REJECTED' THEN 'REJECTED' WHEN 'REJECT' THEN 'REJECTED' WHEN 'DECLINED' THEN 'REJECTED' WHEN 'CANCELLED' THEN 'REJECTED' WHEN 'CANCELED' THEN 'REJECTED'
         WHEN 'EXPIRED' THEN 'EXPIRED'
         ELSE 'PENDING' END,
       coalesce(sr.is_deleted,false), coalesce(sr.created_at, now()), coalesce(sr.updated_at, sr.created_at, now())
FROM migration.stage_permreq sr
JOIN migration.user_id_map mf ON mf.old_id=sr.user_f
JOIN migration.user_id_map mt ON mt.old_id=sr.user_t
ON CONFLICT (id) DO NOTHING;

\echo '>>> APPLY: packages (eski document_package)'
INSERT INTO "user".packages (id, name_uz, name_ru, name_en, description_uz, description_ru, description_en, price, stars, duration, templates, type, deleted, created_date, last_modified_date)
SELECT uuid_generate_v5(uuid_ns_url(), 'hesap:old-docpkg:'||dp.id),
       dp.name_uz, dp.name_ru, dp.name_en, dp.description_uz, dp.description_ru, dp.description_en,
       coalesce(dp.price,0), 0, coalesce(dp.document_count,0), NULL, 'CONTRACT',
       coalesce(dp.deleted,false), coalesce(dp.created_date, now()), now()
FROM migration.stage_docpkg dp
ON CONFLICT (id) DO NOTHING;

\echo '>>> APPLY: user_package'
INSERT INTO "user".user_package (id, package_id, user_in, exp_date, deleted, created_at, updated_at)
SELECT uuid_generate_v5(uuid_ns_url(), 'hesap:old-userpkg:'||up.id),
       uuid_generate_v5(uuid_ns_url(), 'hesap:old-docpkg:'||up.package_id),
       u.pnfl, up.expired_date, false, coalesce(up.created_at, now()), coalesce(up.updated_at, up.created_at, now())
FROM migration.stage_userpkg up
JOIN migration.old_users u ON u.id=up.user_id
JOIN migration.stage_docpkg dp ON dp.id=up.package_id
WHERE u.pnfl IS NOT NULL AND u.pnfl<>''
ON CONFLICT (id) DO NOTHING;

\echo '=== APPLY natijasi (jadval sonlari) ==='
SELECT 'white_list='||count(*) FROM "user".white_list
UNION ALL SELECT 'white_list_request='||count(*) FROM "user".white_list_request
UNION ALL SELECT 'packages='||count(*) FROM "user".packages
UNION ALL SELECT 'user_package='||count(*) FROM "user".user_package;
\endif

COMMIT;
