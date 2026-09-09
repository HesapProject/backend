-- ====================================================================
-- 13-fix-contract-2927.sql
-- Bitta legacy shartnoma (default № 2927) buzuq to'lov jadvalini tuzatish.
--
-- KELIB CHIQISHI (migratsiya artefaktlari):
--   1) VALYUTA: to'lovlar va/yoki shartnomaning o'zi xato valyutada (masalan USD),
--      migratsiyadagi currency_id noto'g'ri biriktirilishi tufayli.
--   2) x100: 07-x100 skripti (2026-07-18) allaqachon TIYINdagi ba'zi summalarni
--      yana ×100 qilgan → summa 100× katta ko'rinadi.
--   3) DUBLIKAT: prepare_id kollisiyasi tufayli qatorlar ikkilangan (24 ≈ 2×12).
--      11-dedup ularni qoldirgan (ikkala nusxa PAID edi) → qo'lda tuzatish shu skript.
--
-- TO'G'RI VALYUTA qanday tanlanadi:
--   * default: to'lovlar orasida ENG KO'P uchraydigan valyuta (mode) — chunki
--     bir nechta qator buzuq bo'lsa ham, ko'pchilik to'g'ri bo'ladi.
--   * -v ccy=UZS berilsa, o'sha majburiy ishlatiladi (preview'ni ko'rib qaror qiling).
--   * HAM to'lovlar, HAM shartnoma shu valyutaga tekislanadi (shartnoma o'zi xato
--     valy%atada bo'lsa ham tuzatiladi).
--
-- XAVFSIZLIK:
--   * PREVIEW-FIRST: -v apply=false (default) → HECH NIMA yozilmaydi.
--   * IDEMPOTENT + QAYTARILADI: eski qiymatlar migration.* log jadvallariga yoziladi;
--     dublikat soft-delete (deleted=true) — qattiq DELETE emas.
--   * MOLIYAVIY BUTUNLIK: real bog'lami (payment_requests/delay_requests/
--     payment_transactions) bo'lgan qator O'CHIRILMAYDI — qo'lda ko'rib chiqishga chiqadi.
--   * Bitta tranzaksiya (ON_ERROR_STOP=1 bilan ishga tushiring).
--
-- ISHGA TUSHIRISH:
--   Preview:  psql ... -v ON_ERROR_STOP=1 -v apply=false -f 13-fix-contract-2927.sql
--   Yozish:   psql ... -v ON_ERROR_STOP=1 -v apply=true  -f 13-fix-contract-2927.sql
--   Valyuta majburiy: qo'shimcha -v ccy=UZS ; boshqa shartnoma: -v num=1234
-- ====================================================================

\if :{?apply}
\else
  \set apply false
\endif
\if :{?num}
\else
  \set num '2927'
\endif

\echo '===================================================================='
\echo 'FIX contract №' :'num' '   apply=' :apply
\echo '===================================================================='

-- ---- Log jadvallari (idempotent) --------------------------------------
CREATE SCHEMA IF NOT EXISTS migration;
CREATE TABLE IF NOT EXISTS migration.fix_c2927_value_log (
  payment_id UUID PRIMARY KEY, contract_id UUID, contract_number TEXT,
  old_total NUMERIC, new_total NUMERIC, old_paid NUMERIC, new_paid NUMERIC,
  old_currency TEXT, new_currency TEXT, reason TEXT,
  fixed_at TIMESTAMP NOT NULL DEFAULT now());
CREATE TABLE IF NOT EXISTS migration.fix_c2927_dedup_log (
  payment_id UUID PRIMARY KEY, contract_id UUID, contract_number TEXT,
  payment_date TIMESTAMP, total_amount NUMERIC, currency TEXT,
  kept_id UUID, deleted_at TIMESTAMP NOT NULL DEFAULT now());
CREATE TABLE IF NOT EXISTS migration.fix_c2927_contract_log (
  contract_id UUID PRIMARY KEY, contract_number TEXT,
  old_currency TEXT, new_currency TEXT, old_currency_id TEXT, new_currency_id TEXT,
  fixed_at TIMESTAMP NOT NULL DEFAULT now());

-- ---- To'g'ri valyutani aniqlash ---------------------------------------
-- auto = SHARTNOMA valyutasi (yagona, avtoritetli — to'lovlar orasidagi teng-holat
-- muammosini yo'q qiladi; yangi backend ham barcha to'lovga shartnoma valyutasini beradi).
-- Agar shartnomaning o'zi xato valyutada bo'lsa, -v ccy=UZS bilan majburiy bering.
SELECT (SELECT currency FROM document.contracts WHERE number = :'num' AND NOT deleted LIMIT 1) AS auto_ccy
\gset
\if :{?ccy}
\else
  \set ccy :auto_ccy
\endif
-- tanlangan valyuta uchun currency_id (o'sha valyutali qatordan; bo'lmasa shartnomadan).
SELECT coalesce(
  (SELECT mode() WITHIN GROUP (ORDER BY p.currency_id)
   FROM document.payments p JOIN document.contracts c ON c.id = p.contract_id
   WHERE c.number = :'num' AND NOT p.deleted AND p.currency = :'ccy'),
  (SELECT currency_id FROM document.contracts WHERE number = :'num' AND NOT deleted LIMIT 1)
) AS ccy_id
\gset

\echo '--- Tanlangan TO''G''RI valyuta ---'
\echo 'auto (ko''pchilik to''lov) =' :'auto_ccy' '   ishlatiladi (ccy) =' :'ccy'

-- ---- Shartnoma --------------------------------------------------------
\echo '--- Shartnoma (valyutasi to''g''rimi?) ---'
SELECT id, number, status, currency AS shartnoma_valyuta, currency_id,
       (price::numeric/100)::numeric(18,2) AS price_som,
       (currency IS DISTINCT FROM :'ccy') AS shartnoma_valyuta_TUZATILADI
FROM document.contracts WHERE number = :'num' AND NOT deleted;

-- ---- Shartnoma ma'lumoti (narx = ×100 tekshiruvi uchun) ---------------
-- ×100 aniqlash: bitta to'lov summasi shartnoma NARXIDAN katta bo'lolmaydi —
-- shuning uchun total_amount > price bo'lgan qator ×100 buzuq (÷100 qilinadi).
-- Bu mode'ga qaraganda barqaror: nomutanosib jadvallarda ham hamma buzuq qatorni tutadi.
DROP TABLE IF EXISTS c2927;
CREATE TEMP TABLE c2927 AS
SELECT c.id AS contract_id, c.number, c.currency AS c_currency, c.price::numeric AS c_price
FROM document.contracts c
WHERE c.number = :'num' AND NOT c.deleted;

-- ---- Joriy to'lovlar + tashxis bayroqlari -----------------------------
DROP TABLE IF EXISTS c2927_pay;
CREATE TEMP TABLE c2927_pay AS
SELECT p.id, p.contract_id, p.contract_payment_date, p.total_amount, p.paid_amount,
       p.currency, p.currency_id, p.status, p.paid_at, p.created_at,
       x.c_price,
       (p.paid_at IS NOT NULL OR p.status = 'PAID')          AS is_paid,
       (x.c_price > 0 AND p.total_amount > x.c_price)        AS needs_x100,
       (p.currency IS DISTINCT FROM :'ccy')                  AS needs_ccy,
       (EXISTS(SELECT 1 FROM document.payment_requests r WHERE r.payment_id=p.id AND NOT r.deleted)
        OR EXISTS(SELECT 1 FROM document.delay_requests d WHERE d.payment_id=p.id AND NOT d.deleted)
        OR EXISTS(SELECT 1 FROM document.payment_transactions t WHERE t.payment_schedule_id=p.id AND NOT t.deleted)
       )                                                     AS has_ref
FROM document.payments p
JOIN c2927 x ON x.contract_id = p.contract_id
WHERE NOT p.deleted;

\echo '--- To''lov valyutalari taqsimoti (hozircha) ---'
SELECT currency AS valyuta, count(*) AS qator,
       (min(total_amount)/100.0)::numeric(18,2) AS eng_kichik_som,
       (max(total_amount)/100.0)::numeric(18,2) AS eng_katta_som,
       (currency IS DISTINCT FROM :'ccy') AS tuzatiladi
FROM c2927_pay GROUP BY currency ORDER BY qator DESC;

\echo '--- Joriy to''lovlar (xom tiyin, so''m, valyuta, bayroqlar) ---'
SELECT contract_payment_date::date AS sana,
       total_amount AS xom_tiyin, (total_amount/100.0)::numeric(18,2) AS som,
       currency, status, is_paid, has_ref,
       needs_x100 AS "×100?", needs_ccy AS "valyuta?"
FROM c2927_pay
ORDER BY contract_payment_date, total_amount DESC;

\echo '--- Umumiy manzara ---'
SELECT count(*) AS jami_qator,
       count(*) FILTER (WHERE needs_x100) AS x100_tuzatiladi,
       count(*) FILTER (WHERE needs_ccy)  AS valyuta_tuzatiladi,
       (sum(CASE WHEN needs_x100 THEN total_amount/100 ELSE total_amount END)/100.0)::numeric(18,2) AS qiymat_tuzatilgach_som
FROM c2927_pay;

-- ---- Dublikat rejasi (qiymat+valyuta tuzatilgandan KEYINGI holatga ko'ra) ----
DROP TABLE IF EXISTS c2927_dedup;
CREATE TEMP TABLE c2927_dedup AS
WITH norm AS (
  SELECT id, contract_id, contract_payment_date,
         CASE WHEN needs_x100 THEN total_amount/100 ELSE total_amount END AS n_total,
         :'ccy'::text AS n_currency,
         is_paid, has_ref, created_at
  FROM c2927_pay )
SELECT n.*,
       row_number() OVER w AS rn,
       first_value(n.id) OVER w AS keep_id,
       count(*) FILTER (WHERE n.has_ref)
         OVER (PARTITION BY n.contract_id, n.contract_payment_date, n.n_total, n.n_currency) AS grp_refs
FROM norm n
WINDOW w AS (PARTITION BY n.contract_id, n.contract_payment_date, n.n_total, n.n_currency
             ORDER BY n.has_ref DESC, n.is_paid DESC, n.created_at ASC NULLS LAST, n.id ASC);

\echo '--- Dublikat: O''CHIRILADI (ortiqcha nusxa, bog''lamsiz) ---'
SELECT contract_payment_date::date AS sana, (n_total/100.0)::numeric(18,2) AS som, n_currency,
       count(*) AS ochiriladigan_qator
FROM c2927_dedup WHERE rn > 1 AND NOT has_ref
GROUP BY 1,2,3 ORDER BY 1;

\echo '--- Dublikat: TEGILMAYDI (ortiqcha, lekin real bog''lam bor → QO''LDA) ---'
SELECT id, contract_payment_date::date AS sana, (n_total/100.0)::numeric(18,2) AS som
FROM c2927_dedup WHERE rn > 1 AND has_ref ORDER BY 2;

\if :apply
\echo '>>> APPLY — yoziladi'
BEGIN;

-- 1) To'lov: eski qiymatlarni logga
INSERT INTO migration.fix_c2927_value_log
 (payment_id, contract_id, contract_number, old_total, new_total, old_paid, new_paid,
  old_currency, new_currency, reason)
SELECT p.id, p.contract_id, :'num',
       p.total_amount,
       CASE WHEN p.needs_x100 THEN p.total_amount/100 ELSE p.total_amount END,
       p.paid_amount,
       CASE WHEN p.needs_x100
              THEN CASE WHEN p.paid_amount >= p.total_amount THEN p.total_amount/100
                        ELSE p.paid_amount/100 END
            ELSE p.paid_amount END,
       p.currency, :'ccy',
       concat_ws('+', CASE WHEN p.needs_x100 THEN 'x100' END,
                       CASE WHEN p.needs_ccy THEN 'currency' END)
FROM c2927_pay p
WHERE p.needs_x100 OR p.needs_ccy
ON CONFLICT (payment_id) DO NOTHING;

-- 2) To'lov: qiymat + valyuta tuzatish
UPDATE document.payments dp
SET total_amount = CASE WHEN p.needs_x100 THEN dp.total_amount/100 ELSE dp.total_amount END,
    paid_amount  = CASE WHEN p.needs_x100
                          THEN CASE WHEN dp.paid_amount >= dp.total_amount THEN dp.total_amount/100
                                    ELSE dp.paid_amount/100 END
                        ELSE dp.paid_amount END,
    currency     = :'ccy',
    currency_id  = :'ccy_id',
    updated_at   = now()
FROM c2927_pay p
WHERE dp.id = p.id AND (p.needs_x100 OR p.needs_ccy);

-- 3) Shartnoma valyutasi ham xato bo'lsa — tuzatish + log
INSERT INTO migration.fix_c2927_contract_log
 (contract_id, contract_number, old_currency, new_currency, old_currency_id, new_currency_id)
SELECT id, :'num', currency, :'ccy', currency_id, :'ccy_id'
FROM document.contracts WHERE number = :'num' AND NOT deleted AND currency IS DISTINCT FROM :'ccy'
ON CONFLICT (contract_id) DO NOTHING;

UPDATE document.contracts
SET currency = :'ccy', currency_id = :'ccy_id', last_modified_date = now()
WHERE number = :'num' AND NOT deleted AND currency IS DISTINCT FROM :'ccy';

-- 4) Dublikatlarni soft-delete (faqat bog'lamsiz ortiqcha) + log
INSERT INTO migration.fix_c2927_dedup_log
 (payment_id, contract_id, contract_number, payment_date, total_amount, currency, kept_id)
SELECT d.id, d.contract_id, :'num', d.contract_payment_date, d.n_total, d.n_currency, d.keep_id
FROM c2927_dedup d WHERE d.rn > 1 AND NOT d.has_ref
ON CONFLICT (payment_id) DO NOTHING;

UPDATE document.payments dp
SET deleted = true, updated_at = now()
FROM c2927_dedup d
WHERE dp.id = d.id AND d.rn > 1 AND NOT d.has_ref;

COMMIT;

\echo '=== APPLY dan keyingi jadval ==='
SELECT p.contract_payment_date::date AS sana,
       (p.total_amount/100.0)::numeric(18,2) AS som, p.currency, p.status
FROM document.payments p JOIN document.contracts c ON c.id = p.contract_id
WHERE c.number = :'num' AND NOT p.deleted
ORDER BY p.contract_payment_date;

\echo '=== Yakuniy: shartnoma + jami (bitta valyuta, narxga mos) ==='
SELECT c.currency AS shartnoma_valyuta,
       (c.price::numeric/100)::numeric(18,2) AS price_som,
       (sum(p.total_amount)/100.0)::numeric(18,2) AS tolovlar_jami_som,
       count(p.*) AS tolovlar,
       count(DISTINCT p.currency) AS valyuta_turi
FROM document.contracts c
JOIN document.payments p ON p.contract_id = c.id AND NOT p.deleted
WHERE c.number = :'num' AND NOT c.deleted
GROUP BY c.currency, c.price;
\else
\echo '>>> PREVIEW — hech nima yozilmadi. Yozish uchun: -v apply=true (kerak bo''lsa -v ccy=UZS)'
\endif
