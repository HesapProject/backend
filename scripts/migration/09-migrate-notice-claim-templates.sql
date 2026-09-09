-- Hamma shartnoma shablonlariga (document.contract_template) Talabnoma va
-- Da'vo arizasi shablonlarini yaratib kiritish (document.notice_template /
-- document.claim_template). Matn shablonning O'Z maydonlaridan (name_uz,
-- seller_name_uz, buyer_name_uz) qurilgan — generic emas, har bir shartnoma
-- turiga moslashgan. Gold-namuna: mavjud (lekin endi bog'lanmagan) qarz
-- talabnoma/da'vo shabloni (parametrlar: notice -> generateChild() bilan mos:
-- noticeNumber/number/createdDate/sellerFullName/buyerFullName/price/currency,
-- claim -> reportNumber/... xuddi shu).
-- Idempotent: deterministik uuid_v5(contract_template_id) + ON CONFLICT DO NOTHING.
-- :apply=false -> PREVIEW (faqat sonlarni ko'rsatadi, yozmaydi).
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

\echo '=== contract_template (PUBLISHED) + notice/claim mavjudligi ==='
SELECT ct.id, ct.name_uz, ct.seller_name_uz, ct.buyer_name_uz,
       (nt.id IS NOT NULL) AS has_notice, (clt.id IS NOT NULL) AS has_claim
FROM document.contract_template ct
LEFT JOIN document.notice_template nt ON nt.contract_template_id = ct.id AND NOT nt.deleted
LEFT JOIN document.claim_template clt ON clt.contract_template_id = ct.id AND NOT clt.deleted
WHERE NOT ct.deleted AND ct.status = 'PUBLISHED';

\echo '=== yaratiladigan notice_template soni ==='
SELECT count(*) FROM document.contract_template ct
WHERE NOT ct.deleted AND ct.status = 'PUBLISHED'
  AND NOT EXISTS (
    SELECT 1 FROM document.notice_template nt
    WHERE nt.contract_template_id = ct.id AND NOT nt.deleted);

\echo '=== yaratiladigan claim_template soni ==='
SELECT count(*) FROM document.contract_template ct
WHERE NOT ct.deleted AND ct.status = 'PUBLISHED'
  AND NOT EXISTS (
    SELECT 1 FROM document.claim_template clt
    WHERE clt.contract_template_id = ct.id AND NOT clt.deleted);

\if :apply

\echo '>>> APPLY: document.notice_template'
INSERT INTO document.notice_template
  (id, contract_template_id, company_id, name_uz, name_ru, name_en,
   template_data, status, deleted, created_date, last_modified_date)
SELECT
  uuid_generate_v5(uuid_ns_url(), 'hesap:notice-template:' || ct.id),
  ct.id,
  ct.company_id,
  'Talabnoma — ' || ct.name_uz,
  'Уведомление — ' || coalesce(nullif(ct.name_ru, ''), ct.name_uz),
  'Notice — ' || coalesce(nullif(ct.name_en, ''), ct.name_uz),
  replace(replace(replace(
    $notice$<?xml version="1.0" encoding="UTF-8"?>
<jasperReport xmlns="http://jasperreports.sourceforge.net/jasperreports"
              name="notice_generated" pageWidth="595" pageHeight="842"
              columnWidth="481" leftMargin="57" rightMargin="57" topMargin="57" bottomMargin="57">
    <parameter name="noticeNumber" class="java.lang.String"/>
    <parameter name="number" class="java.lang.String"/>
    <parameter name="createdDate" class="java.lang.String"/>
    <parameter name="sellerFullName" class="java.lang.String"/>
    <parameter name="buyerFullName" class="java.lang.String"/>
    <parameter name="price" class="java.lang.String"/>
    <parameter name="currency" class="java.lang.String"/>
    <title>
        <band height="400">
            <staticText>
                <reportElement x="0" y="0" width="481" height="20"/>
                <textElement textAlignment="Center"><font fontName="Times New Roman" size="12" isBold="true"/></textElement>
                <text><![CDATA[TALABNOMA]]></text>
            </staticText>
            <textField>
                <reportElement x="0" y="24" width="481" height="16"/>
                <textElement textAlignment="Center"><font fontName="Times New Roman" size="10"/></textElement>
                <textFieldExpression><![CDATA["№ " + $P{noticeNumber} + "  |  Shartnoma № " + $P{number}]]></textFieldExpression>
            </textField>
            <textField isStretchWithOverflow="true">
                <reportElement x="0" y="50" width="481" height="330"/>
                <textElement textAlignment="Justified"><font fontName="Times New Roman" size="10"/></textElement>
                <textFieldExpression><![CDATA["Hurmatli " + $P{buyerFullName} + "!\n\n" +
$P{sellerFullName} + " (" + "__SELLER__" + ") va " + $P{buyerFullName} + " (" + "__BUYER__" + ") o'rtasida " +
$P{createdDate} + " sanasida tuzilgan " + "__NAME__" + " turidagi " + $P{number} +
" raqamli shartnoma bo'yicha " + $P{price} + " " + $P{currency} +
" miqdoridagi majburiyat belgilangan muddatda bajarilmagani sababli, ushbu talabnoma yuborilmoqda.\n\n" +
"Iltimos, ko'rsatilgan summani ushbu talabnoma yuborilgan kundan boshlab 10 (o'n) kalendar kun ichida to'liq bajaring, aks holda masala qonunchilikda belgilangan tartibda sud orqali hal qilinadi."]]></textFieldExpression>
            </textField>
        </band>
    </title>
</jasperReport>$notice$,
    '__NAME__', replace(replace(ct.name_uz, '&', '&amp;'), '"', '')),
    '__SELLER__', replace(replace(ct.seller_name_uz, '&', '&amp;'), '"', '')),
    '__BUYER__', replace(replace(ct.buyer_name_uz, '&', '&amp;'), '"', '')),
  'PUBLISHED',
  FALSE,
  now(),
  now()
FROM document.contract_template ct
WHERE NOT ct.deleted AND ct.status = 'PUBLISHED'
  AND NOT EXISTS (
    SELECT 1 FROM document.notice_template nt
    WHERE nt.contract_template_id = ct.id AND NOT nt.deleted)
ON CONFLICT (id) DO NOTHING;

\echo '>>> APPLY: document.claim_template'
INSERT INTO document.claim_template
  (id, contract_template_id, company_id, name_uz, name_ru, name_en,
   template_data, status, deleted, created_date, last_modified_date)
SELECT
  uuid_generate_v5(uuid_ns_url(), 'hesap:claim-template:' || ct.id),
  ct.id,
  ct.company_id,
  'Da''vo arizasi — ' || ct.name_uz,
  'Иск — ' || coalesce(nullif(ct.name_ru, ''), ct.name_uz),
  'Claim — ' || coalesce(nullif(ct.name_en, ''), ct.name_uz),
  replace(replace(replace(
    $claim$<?xml version="1.0" encoding="UTF-8"?>
<jasperReport xmlns="http://jasperreports.sourceforge.net/jasperreports"
              name="claim_generated" pageWidth="595" pageHeight="842"
              columnWidth="481" leftMargin="57" rightMargin="57" topMargin="57" bottomMargin="57">
    <parameter name="reportNumber" class="java.lang.String"/>
    <parameter name="number" class="java.lang.String"/>
    <parameter name="createdDate" class="java.lang.String"/>
    <parameter name="sellerFullName" class="java.lang.String"/>
    <parameter name="buyerFullName" class="java.lang.String"/>
    <parameter name="price" class="java.lang.String"/>
    <parameter name="currency" class="java.lang.String"/>
    <title>
        <band height="320">
            <staticText>
                <reportElement x="0" y="0" width="481" height="20"/>
                <textElement textAlignment="Center"><font fontName="Times New Roman" size="12" isBold="true"/></textElement>
                <text><![CDATA[DA'VO ARIZASI]]></text>
            </staticText>
            <textField isStretchWithOverflow="true">
                <reportElement x="0" y="30" width="481" height="280"/>
                <textElement textAlignment="Justified"><font fontName="Times New Roman" size="10"/></textElement>
                <textFieldExpression><![CDATA["№ " + $P{reportNumber} + "  |  Shartnoma № " + $P{number} + "\n\n" +
$P{sellerFullName} + " (" + "__SELLER__" + ") va " + $P{buyerFullName} + " (" + "__BUYER__" + ") o'rtasida " +
$P{createdDate} + " sanasida tuzilgan " + "__NAME__" + " turidagi " + $P{number} +
" raqamli shartnoma bo'yicha " + $P{price} + " " + $P{currency} +
" miqdoridagi majburiyat ijro etilmaganligi sababli, ushbu da'vo arizasi taqdim etiladi.\n\n" +
"Sudga: yuqorida ko'rsatilgan shartnoma bo'yicha majburiyatni to'liq bajarishni va yetkazilgan zararni undirishni so'rayman."]]></textFieldExpression>
            </textField>
        </band>
    </title>
</jasperReport>$claim$,
    '__NAME__', replace(replace(ct.name_uz, '&', '&amp;'), '"', '')),
    '__SELLER__', replace(replace(ct.seller_name_uz, '&', '&amp;'), '"', '')),
    '__BUYER__', replace(replace(ct.buyer_name_uz, '&', '&amp;'), '"', '')),
  'PUBLISHED',
  FALSE,
  now(),
  now()
FROM document.contract_template ct
WHERE NOT ct.deleted AND ct.status = 'PUBLISHED'
  AND NOT EXISTS (
    SELECT 1 FROM document.claim_template clt
    WHERE clt.contract_template_id = ct.id AND NOT clt.deleted)
ON CONFLICT (id) DO NOTHING;

\echo '=== YAKUN ==='
SELECT 'notice_template' t, count(*) FROM document.notice_template WHERE NOT deleted
UNION ALL SELECT 'claim_template', count(*) FROM document.claim_template WHERE NOT deleted;

\else
\echo 'PREVIEW rejimi — yozilmadi. Yozish uchun commit xabariga [apply] qo-shing.'
\endif
