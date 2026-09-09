-- Eski Hesap (trust DB) userlarini yangi "user".user jadvaliga ko'chirish.
-- CSV fayllar joylashgan papkadan ishga tushiring:
--   cd old-data && psql "$NEW_DB_URL" -f ../02-import-old-users.sql
--
-- Xususiyatlari:
--   * Idempotent — qayta ishga tushirish xavfsiz (ON CONFLICT / NOT EXISTS).
--   * Deterministik UUID — eski bigint id dan uuid_generate_v5 bilan hosil qilinadi,
--     shuning uchun keyingi bosqichlar (shartnoma, billing) shu map orqali bog'lanadi.
--   * Telefon bo'yicha allaqachon ro'yxatdan o'tgan CLIENT bo'lsa, yangi yozuv
--     yaratilmaydi — mavjud user map ga bog'lanadi (matched_existing = TRUE).
--   * migration.user_id_map — eski id ↔ yangi UUID jadvali saqlanib qoladi.

\set ON_ERROR_STOP on

BEGIN;

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE SCHEMA IF NOT EXISTS migration;

-- 1. Staging jadvallar
CREATE TABLE IF NOT EXISTS migration.old_users (
    id              BIGINT PRIMARY KEY,
    username        TEXT,
    firstname       TEXT,
    lastname        TEXT,
    middlename      TEXT,
    is_man          BOOLEAN,
    bio             TEXT,
    birthday        TEXT,
    phone           TEXT,
    image           TEXT,
    pnfl            TEXT,
    passport_number TEXT,
    type            TEXT,
    tin             TEXT,
    inn             TEXT,
    address         TEXT,
    verified        BOOLEAN,
    verified_at     TIMESTAMP,
    is_deleted      BOOLEAN,
    created_at      TIMESTAMP,
    test_user       BOOLEAN
);

CREATE TABLE IF NOT EXISTS migration.old_socials (
    user_id   BIGINT,
    phone2    TEXT,
    telegram  TEXT,
    instagram TEXT,
    facebook  TEXT
);

TRUNCATE migration.old_users, migration.old_socials;

\copy migration.old_users FROM 'old_users.csv' WITH (FORMAT csv, HEADER true)
\copy migration.old_socials FROM 'old_socials.csv' WITH (FORMAT csv, HEADER true)

-- 2. Eski id ↔ yangi UUID xaritasi
CREATE TABLE IF NOT EXISTS migration.user_id_map (
    old_id           BIGINT PRIMARY KEY,
    new_id           UUID NOT NULL,
    matched_existing BOOLEAN NOT NULL DEFAULT FALSE,
    migrated_at      TIMESTAMP NOT NULL DEFAULT now()
);

-- Telefon raqamni yangi backend formatiga keltirish: faqat raqamlar (+, bo'shliq, - olib tashlanadi)
CREATE OR REPLACE FUNCTION migration.norm_phone(p TEXT) RETURNS TEXT
LANGUAGE sql IMMUTABLE AS
$$ SELECT NULLIF(regexp_replace(coalesce(p, ''), '[^0-9]', '', 'g'), '') $$;

-- 2a. Yangi DB da telefon bo'yicha allaqachon mavjud CLIENT larga bog'lash
INSERT INTO migration.user_id_map (old_id, new_id, matched_existing)
SELECT DISTINCT ON (o.id) o.id, u.id, TRUE
FROM migration.old_users o
JOIN "user"."user" u
  ON u.phone = migration.norm_phone(o.phone)
 AND u.type = 'CLIENT'
 AND u.deleted = FALSE
WHERE coalesce(o.is_deleted, FALSE) = FALSE
ORDER BY o.id, u.created_date
ON CONFLICT (old_id) DO NOTHING;

-- 2b. Qolganlarga deterministik UUID (uuid_ns_url namespace + 'hesap:old-user:<id>')
INSERT INTO migration.user_id_map (old_id, new_id, matched_existing)
SELECT o.id,
       uuid_generate_v5(uuid_ns_url(), 'hesap:old-user:' || o.id),
       FALSE
FROM migration.old_users o
WHERE NOT EXISTS (SELECT 1 FROM migration.user_id_map m WHERE m.old_id = o.id);

-- 3. Userlarni ko'chirish — eski DB'da faqat jismoniy shaxslar bor, hammasi CLIENT bo'ladi.
--    O'chirilgan (is_deleted) userlar ham deleted=TRUE bilan ko'chiriladi —
--    eski shartnomalardagi FK butunligi uchun.
INSERT INTO "user"."user" (
    id, username, first_name, last_name, mid_name, phone, pinfl, passport,
    type, image, bio, birthday, is_man, tin, address,
    is_verified, deleted, created_date, last_modified_date, version
)
SELECT
    m.new_id,
    -- username: aktiv yangi userda band bo'lsa NULL (partial unique index buzilmasin)
    CASE
      WHEN o.username IS NULL OR trim(o.username) = '' THEN NULL
      WHEN coalesce(o.is_deleted, FALSE) = FALSE
       AND EXISTS (SELECT 1 FROM "user"."user" e
                   WHERE lower(e.username) = lower(trim(o.username)) AND e.deleted = FALSE)
        THEN NULL
      ELSE lower(trim(o.username))
    END,
    NULLIF(trim(o.firstname), ''),
    NULLIF(trim(o.lastname), ''),
    NULLIF(trim(o.middlename), ''),
    migration.norm_phone(o.phone),
    NULLIF(trim(o.pnfl), ''),
    NULLIF(trim(o.passport_number), ''),
    'CLIENT',
    NULLIF(trim(o.image), ''),
    NULLIF(trim(o.bio), ''),
    NULLIF(trim(o.birthday), ''),
    o.is_man,
    coalesce(NULLIF(trim(o.tin), ''), NULLIF(trim(o.inn), '')),
    NULLIF(trim(o.address), ''),
    coalesce(o.verified, FALSE),
    coalesce(o.is_deleted, FALSE),
    coalesce(o.created_at, now()),
    now(),
    0
FROM migration.old_users o
JOIN migration.user_id_map m ON m.old_id = o.id AND m.matched_existing = FALSE
WHERE NOT EXISTS (SELECT 1 FROM "user"."user" u WHERE u.id = m.new_id);

-- 4. Socials (faqat kamida bitta maydoni to'la bo'lganlar).
-- Sxema 20260622'da o'zgargan: user_id o'rniga user_in (pinfl).
INSERT INTO "user".socials (id, user_in, phone2, telegram, instagram, facebook)
SELECT uuid_generate_v5(uuid_ns_url(), 'hesap:old-social:'||s.user_id),
       u.pinfl,
       NULLIF(trim(s.phone2), ''),
       NULLIF(trim(s.telegram), ''),
       NULLIF(trim(s.instagram), ''),
       NULLIF(trim(s.facebook), '')
FROM migration.old_socials s
JOIN migration.user_id_map m ON m.old_id = s.user_id
JOIN "user"."user" u ON u.id = m.new_id
WHERE u.pinfl IS NOT NULL
  AND coalesce(NULLIF(trim(s.phone2), ''), NULLIF(trim(s.telegram), ''),
               NULLIF(trim(s.instagram), ''), NULLIF(trim(s.facebook), '')) IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM "user".socials x WHERE x.user_in = u.pinfl)
ON CONFLICT (id) DO NOTHING;

-- 4a. second_phone (VARCHAR(20) — uzunlikdan oshganlari tushib qoladi)
UPDATE "user"."user" u
SET second_phone = migration.norm_phone(s.phone2)
FROM migration.old_socials s
JOIN migration.user_id_map m ON m.old_id = s.user_id
WHERE u.id = m.new_id
  AND u.second_phone IS NULL
  AND migration.norm_phone(s.phone2) IS NOT NULL
  AND length(migration.norm_phone(s.phone2)) <= 20;

-- 5. Hisobot
SELECT 'eski userlar (CSV)'                          AS metric, count(*) FROM migration.old_users
UNION ALL
SELECT 'map: mavjudiga bog''langan (phone match)',           count(*) FROM migration.user_id_map WHERE matched_existing
UNION ALL
SELECT 'map: yangi yaratilgan UUID',                          count(*) FROM migration.user_id_map WHERE NOT matched_existing
UNION ALL
SELECT 'yangi DB dagi ko''chirilgan userlar',                 count(*)
FROM "user"."user" u JOIN migration.user_id_map m ON m.new_id = u.id AND NOT m.matched_existing;

-- Telefon dublikatlari (normalizatsiyadan keyin bir xil bo'lib qolganlar) — qo'lda tekshirish uchun
SELECT u.phone, count(*) AS cnt, array_agg(u.id) AS user_ids
FROM "user"."user" u
WHERE u.deleted = FALSE AND u.type = 'CLIENT' AND u.phone IS NOT NULL
GROUP BY u.phone
HAVING count(*) > 1
ORDER BY cnt DESC
LIMIT 50;

COMMIT;
