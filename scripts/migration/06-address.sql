-- To'liq manzil backfill: eski passport_full_data JSON'dan viloyat+tuman+MFY+ko'cha.
\set ON_ERROR_STOP on
BEGIN;
CREATE TABLE IF NOT EXISTS migration.stage_addr (pinfl text, region text, district text, mfy text, street text);
TRUNCATE migration.stage_addr;
\copy migration.stage_addr FROM '/mig/addr.csv' WITH (FORMAT csv, HEADER true)

-- to'liq manzil matni (bo'sh qismlarni tashlab)
DROP TABLE IF EXISTS addr2;
CREATE TEMP TABLE addr2 AS
SELECT pinfl,
  concat_ws(', ',
    nullif(trim(region),''), nullif(trim(district),''), nullif(trim(mfy),''), nullif(trim(street),'')
  ) AS full_addr,
  region, district
FROM migration.stage_addr
WHERE nullif(trim(pinfl),'') IS NOT NULL AND nullif(trim(region),'') IS NOT NULL;

\echo '=== staging: passport_full_data dan olingan (region bor) ==='
SELECT count(*) FROM migration.stage_addr;
SELECT 'full_addr_tayyor='||count(*) FROM addr2;
\echo '=== yangi userlarga PINFL bo-yicha mos ==='
SELECT count(*) FROM addr2 a JOIN "user"."user" u ON u.pinfl=a.pinfl;
\echo '=== shundan address TO-LIQMAS (viloyat yo-q yoki bo-sh) ==='
SELECT count(*) FROM addr2 a JOIN "user"."user" u ON u.pinfl=a.pinfl
 WHERE u.address IS NULL OR u.address='' OR u.address NOT ILIKE '%'||a.region||'%';
\echo '=== namuna: hozirgi -> to-liq ==='
SELECT left(coalesce(u.address,'(bo\x27sh)'),45) AS hozir, left(a.full_addr,90) AS toliq
FROM addr2 a JOIN "user"."user" u ON u.pinfl=a.pinfl
 WHERE u.address IS NULL OR u.address='' OR u.address NOT ILIKE '%'||a.region||'%' LIMIT 12;

\if :apply
\echo '>>> APPLY: address backfill'
UPDATE "user"."user" u SET address=a.full_addr, last_modified_date=now()
FROM addr2 a
WHERE u.pinfl=a.pinfl
  AND (u.address IS NULL OR u.address='' OR u.address NOT ILIKE '%'||a.region||'%');
\echo '=== natija: address to-ldirilgan jami ==='
SELECT count(*) FILTER (WHERE address IS NOT NULL AND address<>'') AS address_bor, count(*) jami FROM "user"."user";
\endif
COMMIT;
