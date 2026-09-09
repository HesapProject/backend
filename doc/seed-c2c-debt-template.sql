-- =============================================================================
-- C2C QARZ SHARTNOMASI — template seed (eski DebtDocument / DebtDocumentTexts)
-- Eski kodga tegilmaydi. Faqat document.template va document.template_field.
--
-- Ishlatish:
--   psql -h localhost -p 5435 -U postgres -d hesap -f doc/seed-c2c-debt-template.sql
--
-- UUID lar (keyingi referens uchun):
--   C2C  : a1000001-0001-4001-8001-000000000001
--   NOTICE: a1000001-0001-4001-8001-000000000002  (template_id -> C2C)
--   REPORT: a1000001-0001-4001-8001-000000000003  (template_id -> C2C)
--
-- key_name ↔ yangi kod mapping (template_field.key_name = JRXML $P{...}):
--   number              ← DocumentEntity.number              [DocumentGenerator AVTO]
--   paymentScheduleList ← payment_schedule jadvali           [DocumentGenerator AVTO]
--   createdDate         ← DocumentEntity.createdDate         [values — formatlangan matn]
--   address             ← OneIdPassportResponse.address
--   sellerFullName      ← OneIdPassportResponse.fullName / seller
--   buyerFullName       ← OneIdPassportResponse.fullName / buyer
--   price               ← DocumentEntity.price (PDF uchun formatlangan matn)
--   currency            ← DocumentEntity.currency (UZS|USD|RUB)
--   priceWords          ← summa so'z bilan (custom)
--   sellerDocument      ← UserResponse.document (pasport)
--   buyerDocument       ← UserResponse.document
--   sellerPinfl         ← UserResponse.pinfl
--   buyerPinfl          ← UserResponse.pinfl
--   sellerPhone         ← UserResponse.phone / seller.phone
--   buyerPhone          ← UserResponse.phone / buyer.phone
--
-- Eski DebtDocument nomlari ISHLATILMAYDI: lender, debtor, createdAt, amount, passport
-- =============================================================================

BEGIN;

-- ---------------------------------------------------------------------------
-- 1) C2C — asosiy qarz shartnomasi
-- ---------------------------------------------------------------------------
INSERT INTO document.template (
    id,
    company_id,
    name_uz,
    name_ru,
    name_en,
    template_data,
    status,
    template_type,
    template_id,
    amount,
    witness_count,
    deleted,
    created_date,
    last_modified_date
) VALUES (
    'a1000001-0001-4001-8001-000000000001',
    NULL,
    'C2C qarz shartnomasi',
    'C2C договор займа',
    'C2C loan agreement',
    $JRXML$<?xml version="1.0" encoding="UTF-8"?>
<jasperReport xmlns="http://jasperreports.sourceforge.net/jasperreports"
              xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xsi:schemaLocation="http://jasperreports.sourceforge.net/jasperreports http://jasperreports.sourceforge.net/xsd/jasperreport.xsd"
              name="c2c_debt_contract_uz"
              pageWidth="595" pageHeight="842"
              columnWidth="481" leftMargin="57" rightMargin="57" topMargin="57" bottomMargin="57"
              uuid="a1000001-0001-4001-8001-000000000001">

    <property name="net.sf.jasperreports.awt.ignore.missing.font" value="true"/>

    <style name="Title" fontName="Times New Roman" fontSize="12" isBold="true"/>
    <style name="Section" fontName="Times New Roman" fontSize="10" isBold="true"/>
    <style name="Body" fontName="Times New Roman" fontSize="10" isBold="false"/>
    <style name="TableHeader" fontName="Times New Roman" fontSize="9" isBold="true"/>
    <style name="TableCell" fontName="Times New Roman" fontSize="9"/>

    <parameter name="number" class="java.lang.String"/>
    <parameter name="address" class="java.lang.String"/>
    <parameter name="createdDate" class="java.lang.String"/>
    <parameter name="sellerFullName" class="java.lang.String"/>
    <parameter name="buyerFullName" class="java.lang.String"/>
    <parameter name="price" class="java.lang.String"/>
    <parameter name="currency" class="java.lang.String"/>
    <parameter name="priceWords" class="java.lang.String"/>
    <parameter name="sellerDocument" class="java.lang.String"/>
    <parameter name="sellerPinfl" class="java.lang.String"/>
    <parameter name="sellerPhone" class="java.lang.String"/>
    <parameter name="buyerDocument" class="java.lang.String"/>
    <parameter name="buyerPinfl" class="java.lang.String"/>
    <parameter name="buyerPhone" class="java.lang.String"/>
    <parameter name="paymentScheduleList" class="java.util.List"/>

    <field name="no" class="java.lang.String"/>
    <field name="toPay" class="java.lang.String"/>
    <field name="paidPart" class="java.lang.String"/>
    <field name="returnTime" class="java.lang.String"/>
    <field name="changeDate" class="java.lang.String"/>
    <field name="status" class="java.lang.String"/>
    <field name="paidTime" class="java.lang.String"/>

    <title>
        <band height="720" splitType="Stretch">
            <staticText>
                <reportElement x="0" y="0" width="481" height="20" uuid="t1"/>
                <textElement textAlignment="Center"><font fontName="Times New Roman" size="12" isBold="true"/></textElement>
                <text><![CDATA[Hesap]]></text>
            </staticText>

            <textField>
                <reportElement x="0" y="24" width="481" height="20" uuid="t2"/>
                <textElement textAlignment="Center"><font fontName="Times New Roman" size="12" isBold="true"/></textElement>
                <textFieldExpression><![CDATA["QARZ SHARTNOMASI №" + $P{number}]]></textFieldExpression>
            </textField>

            <textField>
                <reportElement x="0" y="48" width="240" height="16" uuid="t3"/>
                <textElement><font fontName="Times New Roman" size="10"/></textElement>
                <textFieldExpression><![CDATA[$P{address}]]></textFieldExpression>
            </textField>
            <textField>
                <reportElement x="241" y="48" width="240" height="16" uuid="t4"/>
                <textElement textAlignment="Right"><font fontName="Times New Roman" size="10"/></textElement>
                <textFieldExpression><![CDATA[$P{createdDate}]]></textFieldExpression>
            </textField>

            <textField isStretchWithOverflow="true">
                <reportElement x="0" y="70" width="481" height="50" uuid="t5"/>
                <textElement textAlignment="Justified"><font fontName="Times New Roman" size="10"/></textElement>
                <textFieldExpression><![CDATA["Bir tomondan fuqaro " + $P{sellerFullName} + " (keyingi o‘rinlarda \"Qarz beruvchi\" deb yuritiladi), ikkinchi tomondan fuqaro " + $P{buyerFullName} + " (keyingi o‘rinlarda \"Qarz oluvchi\" deb yuritiladi) birgalikda Tomonlar, alohida-alohida — Tomon deb yuritilgan holda quyidagilar haqida shartnoma tuzdilar:"]]></textFieldExpression>
            </textField>

            <staticText>
                <reportElement x="0" y="126" width="481" height="16" uuid="s1"/>
                <textElement textAlignment="Center"><font fontName="Times New Roman" size="10" isBold="true"/></textElement>
                <text><![CDATA[1. SHARTNOMA PREDMETI]]></text>
            </staticText>

            <textField isStretchWithOverflow="true">
                <reportElement x="0" y="144" width="481" height="40" uuid="s11"/>
                <textElement textAlignment="Justified"><font fontName="Times New Roman" size="10"/></textElement>
                <textFieldExpression><![CDATA["1.1. Ushbu shartnoma shartlariga ko‘ra, Qarz beruvchi Qarz oluvchiga " + $P{price} + " " + $P{currency} + " (" + $P{priceWords} + ") miqdorida qarz beradi, Qarz oluvchi esa ushbu summani Shartnomada ko‘rsatilgan muddatda Qarz beruvchiga qaytarishni o‘z zimmasiga oladi."]]></textFieldExpression>
            </textField>

            <staticText>
                <reportElement x="0" y="188" width="481" height="14" uuid="s12"/>
                <textElement textAlignment="Justified"><font fontName="Times New Roman" size="10"/></textElement>
                <text><![CDATA[1.2. Qarz foizsiz ravishda beriladi.]]></text>
            </staticText>

            <staticText>
                <reportElement x="0" y="206" width="481" height="28" uuid="s13"/>
                <textElement textAlignment="Justified"><font fontName="Times New Roman" size="10"/></textElement>
                <text><![CDATA[1.3. Shartnomaning 1.1-bandida ko‘rsatilgan qarz summasi Qarz oluvchiga 1 (bir) kalendar kun muddatga quyidagi shartlar asosida taqdim etiladi:]]></text>
            </staticText>

            <!-- To'lov jadvali -->
            <staticText><reportElement x="0" y="240" width="30" height="14" uuid="h1"/><textElement textAlignment="Center"><font fontName="Times New Roman" size="9" isBold="true"/></textElement><text><![CDATA[№]]></text></staticText>
            <staticText><reportElement x="30" y="240" width="70" height="14" uuid="h2"/><textElement textAlignment="Center"><font fontName="Times New Roman" size="9" isBold="true"/></textElement><text><![CDATA[To'lov summasi]]></text></staticText>
            <staticText><reportElement x="100" y="240" width="70" height="14" uuid="h3"/><textElement textAlignment="Center"><font fontName="Times New Roman" size="9" isBold="true"/></textElement><text><![CDATA[To'langan summa]]></text></staticText>
            <staticText><reportElement x="170" y="240" width="80" height="14" uuid="h4"/><textElement textAlignment="Center"><font fontName="Times New Roman" size="9" isBold="true"/></textElement><text><![CDATA[Qaytarish vaqti]]></text></staticText>
            <staticText><reportElement x="250" y="240" width="70" height="14" uuid="h5"/><textElement textAlignment="Center"><font fontName="Times New Roman" size="9" isBold="true"/></textElement><text><![CDATA[O'zgargan sana]]></text></staticText>
            <staticText><reportElement x="320" y="240" width="70" height="14" uuid="h6"/><textElement textAlignment="Center"><font fontName="Times New Roman" size="9" isBold="true"/></textElement><text><![CDATA[Holati]]></text></staticText>
            <staticText><reportElement x="390" y="240" width="91" height="14" uuid="h7"/><textElement textAlignment="Center"><font fontName="Times New Roman" size="9" isBold="true"/></textElement><text><![CDATA[To'langan vaqti]]></text></staticText>

            <componentElement>
                <reportElement x="0" y="256" width="481" height="60" uuid="paylist"/>
                <jr:list xmlns:jr="http://jasperreports.sourceforge.net/jasperreports/components" xsi:schemaLocation="http://jasperreports.sourceforge.net/jasperreports/components http://jasperreports.sourceforge.net/xsd/components.xsd">
                    <datasetRun subDataset="paymentDataset">
                        <dataSourceExpression><![CDATA[new net.sf.jasperreports.engine.data.JRMapCollectionDataSource($P{paymentScheduleList})]]></dataSourceExpression>
                    </datasetRun>
                    <jr:listContents height="14" width="481">
                        <textField><reportElement x="0" y="0" width="30" height="14"/><textElement textAlignment="Center"><font fontName="Times New Roman" size="9"/></textElement><textFieldExpression><![CDATA[$F{no}]]></textFieldExpression></textField>
                        <textField><reportElement x="30" y="0" width="70" height="14"/><textElement textAlignment="Center"><font fontName="Times New Roman" size="9"/></textElement><textFieldExpression><![CDATA[$F{toPay}]]></textFieldExpression></textField>
                        <textField><reportElement x="100" y="0" width="70" height="14"/><textElement textAlignment="Center"><font fontName="Times New Roman" size="9"/></textElement><textFieldExpression><![CDATA[$F{paidPart}]]></textFieldExpression></textField>
                        <textField><reportElement x="170" y="0" width="80" height="14"/><textElement textAlignment="Center"><font fontName="Times New Roman" size="9"/></textElement><textFieldExpression><![CDATA[$F{returnTime}]]></textFieldExpression></textField>
                        <textField><reportElement x="250" y="0" width="70" height="14"/><textElement textAlignment="Center"><font fontName="Times New Roman" size="9"/></textElement><textFieldExpression><![CDATA[$F{changeDate}]]></textFieldExpression></textField>
                        <textField><reportElement x="320" y="0" width="70" height="14"/><textElement textAlignment="Center"><font fontName="Times New Roman" size="9"/></textElement><textFieldExpression><![CDATA[$F{status}]]></textFieldExpression></textField>
                        <textField><reportElement x="390" y="0" width="91" height="14"/><textElement textAlignment="Center"><font fontName="Times New Roman" size="9"/></textElement><textFieldExpression><![CDATA[$F{paidTime}]]></textFieldExpression></textField>
                    </jr:listContents>
                </jr:list>
            </componentElement>

            <staticText>
                <reportElement x="0" y="330" width="481" height="380" uuid="legal"/>
                <textElement textAlignment="Justified"><font fontName="Times New Roman" size="10"/></textElement>
                <text><![CDATA[2. QARZ SUMMASINI TOPSHIRISH TARTIBI
2.1. Ushbu shartnoma tomonlar tomonidan imzolangan paytdan boshlab kuchga kiradi.
2.2. Qarz summasi Qarz beruvchi tomonidan Qarz oluvchiga ushbu shartnoma imzolangan kundan boshlab bir kalendar kuni ichida naqd shaklda yoki ushbu shartnomada ko‘rsatilgan Qarz oluvchining bank plastik kartasiga pul o‘tkazish yo‘li bilan topshiriladi.
2.3. Qarz berilgan sana deb, agar pul naqd shaklda berilgan bo‘lsa – uni topshirgan sana, bank plastik kartaga o‘tkazilgan bo‘lsa – pul mablag‘lari kartaga tushgan sana hisoblanadi.
2.4. Taraflar o‘rtasida hisob-kitob O‘zbekiston Respublikasi milliy valyutasida naqd pul yoki bank orqali pul o‘tkazish yo‘li bilan amalga oshiriladi.

3. QARZ SUMMASINI QAYTARISH TARTIBI
3.1. Qarz oluvchi qarz summasini to‘liq hajmda Qarz beruvchiga, ushbu shartnomaning 1.3 bandida ko‘rsatilgan muddat tugagan kunning ertasidan kechiktirmasdan qaytarishni o‘z zimmasiga oladi.
3.2. Qarz oluvchi tomonidan qarz summasini Qarz beruvchiga qaytarish majburiyatining bajarilgan sanasi, agar naqd shaklda to‘langan bo‘lsa — pulni berish sanasi yoki bank plastik kartaga pul tushirilgan bo‘lsa — mablag‘ning Qarz beruvchining kartasiga tushgan sanasi hisoblanadi.
3.3. Qarz oluvchining bank kartasidan mablag‘ yechilgan bo‘lsa-da, agar bu mablag‘lar Qarz beruvchining bank kartasiga yetib bormagan bo‘lsa, bu holat Qarz oluvchini qarzni qaytarish majburiyatidan ozod qilmaydi.

4. TOMONLARNING HUQUQLARI VA MAJBURIYATLARI
4.1. Qarz beruvchining huquqi:
• Qarz beruvchi Qarz oluvchidan shartnoma bo‘yicha berilgan qarz summasini shartnomada kelishilgan muddatdan avval talab qilishga haqli agar Qarz oluvchi qarz summasini qaytarish muddatini bir marta kechiktirsa.
4.2. Qarz beruvchining majburiyati:
• Qarz beruvchi mazkur shartnomaning 2.2. bandiga asosan Qarz oluvchiga qarz summasini topshirish.
4.3. Qarz oluvchining huquqi:
• Qarz summasini Qarz beruvchining roziligi bilan muddatidan oldin qaytarish.
4.4. Qarz oluvchining majburiyati:
• Qarz summasini ushbu shartnomaning 1.3. bandida belgilangan muddatlarda Qarz beruvchiga qaytarib berish yoki qarzni qaytarishning kelishilgan muddati tugagach Qarz beruvchidan olingan qarz summasini qaytarish.

5. TOMONLARNING JAVOBGARLIGI, NIZOLARNI HAL QILISH TARTIBI
5.1. Tomonlar o‘z majburiyatlarini bajarmagan yoki lozim darajada bajarmaganliklari uchun O‘zbekiston Respublikasining Fuqarolik kodeksi va boshqa qonun hujjatlari hamda mazkur shartnomaga muvofiq javobgar bo‘ladilar.
5.2. Tomonlar o‘rtasida kelib chiqadigan nizolar tomonlarning o‘zaro kelishuvi asosida hal etiladi.
5.3. Tomonlar kelishuvga erishmagan taqdirda, mazkur shartnomaning tuzilishi, uning shartlarining o‘zgarishi, buzilishi, ijro etilishi, bekor bo‘lishi, tugatilishi va haqiqiyligi yuzasidan kelib chiquvchi barcha nizolar, kelishmovchiliklar O`zbekiston Respublikasining amaldagi qonunchiligiga muvofiq ko‘rib chiqiladi.

6. FORS-MAJOR HOLATLAR
6.1. Tomonlarning hech biri ikkinchi tomonga nisbatan, o‘z irodasi va xohishiga bog‘liq bo‘lmagan, oldindan ko‘rib bo‘lmaydigan yoki oldini olib bo‘lmaydigan holatlar sababli yuzaga kelgan majburiyatlarni bajarmaganligi uchun javobgar bo‘lmaydi.
6.2. Fors-major holatlaridan zarar ko‘rgan Tomon bunday holatlar yuzaga kelgan kundan boshlab 5 kalendar kuni ichida boshqa Tomonni bu haqida har qanday aloqa vositalari orqali xabardor qilishi shart.

7. YAKUNIY QOIDALAR
7.1–7.9. Ushbu shartnoma tomonlar tomonidan elektron shaklda imzolangan paytdan boshlab kuchga kiradi va “Hesap” elektron platformasi, “MY ID” identifikatsiyasi hamda SMS-xabarnoma orqali tuzilgan deb hisoblanadi.

8. TOMONLARNING REKVIZITLARI]]></text>
            </staticText>
        </band>
    </title>

    <summary>
        <band height="180" splitType="Stretch">
            <staticText>
                <reportElement x="0" y="0" width="240" height="14" uuid="f1"/>
                <textElement textAlignment="Center"><font fontName="Times New Roman" size="10" isBold="true"/></textElement>
                <text><![CDATA[QARZ BERUVCHI]]></text>
            </staticText>
            <staticText>
                <reportElement x="241" y="0" width="240" height="14" uuid="f2"/>
                <textElement textAlignment="Center"><font fontName="Times New Roman" size="10" isBold="true"/></textElement>
                <text><![CDATA[QARZ OLUVCHI]]></text>
            </staticText>

            <textField isStretchWithOverflow="true">
                <reportElement x="0" y="18" width="240" height="80" uuid="f3"/>
                <textElement><font fontName="Times New Roman" size="10"/></textElement>
                <textFieldExpression><![CDATA["F.I.SH: " + $P{sellerFullName} + "\nPasport seriya: " + $P{sellerDocument} + "\nJSHSHIR: " + $P{sellerPinfl} + "\nTel: " + $P{sellerPhone}]]></textFieldExpression>
            </textField>
            <textField isStretchWithOverflow="true">
                <reportElement x="241" y="18" width="240" height="80" uuid="f4"/>
                <textElement><font fontName="Times New Roman" size="10"/></textElement>
                <textFieldExpression><![CDATA["F.I.SH: " + $P{buyerFullName} + "\nPasport seriya: " + $P{buyerDocument} + "\nJSHSHIR: " + $P{buyerPinfl} + "\nTel: " + $P{buyerPhone}]]></textFieldExpression>
            </textField>
        </band>
    </summary>

    <subDataset name="paymentDataset" uuid="pay-ds">
        <field name="no" class="java.lang.String"/>
        <field name="toPay" class="java.lang.String"/>
        <field name="paidPart" class="java.lang.String"/>
        <field name="returnTime" class="java.lang.String"/>
        <field name="changeDate" class="java.lang.String"/>
        <field name="status" class="java.lang.String"/>
        <field name="paidTime" class="java.lang.String"/>
    </subDataset>
</jasperReport>$JRXML$,
    'PUBLISHED',
    'C2C',
    NULL,
    NULL,
    0,
    FALSE,
    NOW(),
    NOW()
);

-- ---------------------------------------------------------------------------
-- 2) template_field — JRXML $P{...} parametrlari (document_value orqali to'ldiriladi)
--    Eslatma: number va paymentScheduleList DocumentGenerator avtomatik qo'shadi
-- ---------------------------------------------------------------------------
INSERT INTO document.template_field
    (id, template_id, key_name, name_uz, name_ru, name_en, type, position, parent_key, deleted, created_date, last_modified_date)
VALUES
    ('a1000002-0001-4001-8001-000000000001', 'a1000001-0001-4001-8001-000000000001', 'address',        'Manzil / shahar',      'Адрес',              'Address',           'STRING', 'BOTTOM', NULL, FALSE, NOW(), NOW()),
    ('a1000002-0001-4001-8001-000000000002', 'a1000001-0001-4001-8001-000000000001', 'createdDate',    'Shartnoma sanasi',     'Дата договора',      'Created date',      'STRING', 'BOTTOM', NULL, FALSE, NOW(), NOW()),
    ('a1000002-0001-4001-8001-000000000003', 'a1000001-0001-4001-8001-000000000001', 'sellerFullName', 'Sotuvchi F.I.Sh',      'ФИО продавца',       'Seller full name',  'STRING', 'BOTTOM', NULL, FALSE, NOW(), NOW()),
    ('a1000002-0001-4001-8001-000000000004', 'a1000001-0001-4001-8001-000000000001', 'buyerFullName',  'Xaridor F.I.Sh',       'ФИО покупателя',     'Buyer full name',   'STRING', 'BOTTOM', NULL, FALSE, NOW(), NOW()),
    ('a1000002-0001-4001-8001-000000000005', 'a1000001-0001-4001-8001-000000000001', 'price',          'Summa (formatlangan)', 'Сумма',              'Price formatted',   'STRING', 'BOTTOM', NULL, FALSE, NOW(), NOW()),
    ('a1000002-0001-4001-8001-000000000006', 'a1000001-0001-4001-8001-000000000001', 'currency',       'Valyuta',              'Валюта',             'Currency',          'STRING', 'BOTTOM', NULL, FALSE, NOW(), NOW()),
    ('a1000002-0001-4001-8001-000000000007', 'a1000001-0001-4001-8001-000000000001', 'priceWords',     'Summa so''z bilan',    'Сумма прописью',     'Price in words',    'STRING', 'BOTTOM', NULL, FALSE, NOW(), NOW()),
    ('a1000002-0001-4001-8001-000000000008', 'a1000001-0001-4001-8001-000000000001', 'sellerDocument', 'Sotuvchi pasport',     'Паспорт продавца',   'Seller document',   'STRING', 'BOTTOM', NULL, FALSE, NOW(), NOW()),
    ('a1000002-0001-4001-8001-000000000009', 'a1000001-0001-4001-8001-000000000001', 'sellerPinfl',    'Sotuvchi PINFL',       'ПИНФЛ продавца',     'Seller PINFL',      'STRING', 'BOTTOM', NULL, FALSE, NOW(), NOW()),
    ('a1000002-0001-4001-8001-000000000010', 'a1000001-0001-4001-8001-000000000001', 'sellerPhone',    'Sotuvchi telefon',     'Тел. продавца',      'Seller phone',      'STRING', 'BOTTOM', NULL, FALSE, NOW(), NOW()),
    ('a1000002-0001-4001-8001-000000000011', 'a1000001-0001-4001-8001-000000000001', 'buyerDocument',  'Xaridor pasport',      'Паспорт покупателя', 'Buyer document',    'STRING', 'BOTTOM', NULL, FALSE, NOW(), NOW()),
    ('a1000002-0001-4001-8001-000000000012', 'a1000001-0001-4001-8001-000000000001', 'buyerPinfl',     'Xaridor PINFL',        'ПИНФЛ покупателя',   'Buyer PINFL',       'STRING', 'BOTTOM', NULL, FALSE, NOW(), NOW()),
    ('a1000002-0001-4001-8001-000000000013', 'a1000001-0001-4001-8001-000000000001', 'buyerPhone',     'Xaridor telefon',      'Тел. покупателя',    'Buyer phone',       'STRING', 'BOTTOM', NULL, FALSE, NOW(), NOW());

-- ---------------------------------------------------------------------------
-- 3) NOTICE — C2C ga bog'langan talabnoma shabloni (keyinroq to'ldirasiz)
-- ---------------------------------------------------------------------------
INSERT INTO document.template (
    id, company_id, name_uz, name_ru, name_en, template_data, status, template_type,
    template_id, amount, witness_count, deleted, created_date, last_modified_date
) VALUES (
    'a1000001-0001-4001-8001-000000000002',
    NULL,
    'C2C qarz talabnomasi',
    'C2C претензия по займу',
    'C2C loan notice',
    $NOTICE$<?xml version="1.0" encoding="UTF-8"?>
<jasperReport xmlns="http://jasperreports.sourceforge.net/jasperreports"
              name="c2c_debt_notice_uz" pageWidth="595" pageHeight="842"
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
                <reportElement x="0" y="50" width="481" height="300"/>
                <textElement textAlignment="Justified"><font fontName="Times New Roman" size="10"/></textElement>
                <textFieldExpression><![CDATA["Hurmatli " + $P{buyerFullName} + "!\n\n" +
"Siz bilan " + $P{sellerFullName} + " o'rtasida " + $P{createdDate} + " sanasida tuzilgan " +
$P{number} + " raqamli qarz shartnomasi bo'yicha " + $P{price} + " " + $P{currency} +
" miqdoridagi qarzni belgilangan muddatda to'lamaganingiz sababli, ushbu talabnoma yuborilmoqda.\n\n" +
"Iltimos, qarz summasini 10 (o'n) kalendar kun ichida to'liq qoplang."]]]></textFieldExpression>
            </textField>
        </band>
    </title>
</jasperReport>$NOTICE$,
    'PUBLISHED',
    'NOTICE',
    'a1000001-0001-4001-8001-000000000001',
    NULL,
    NULL,
    FALSE,
    NOW(),
    NOW()
);

-- ---------------------------------------------------------------------------
-- 4) REPORT — da'vo arizasi (stub)
-- ---------------------------------------------------------------------------
INSERT INTO document.template (
    id, company_id, name_uz, name_ru, name_en, template_data, status, template_type,
    template_id, amount, witness_count, deleted, created_date, last_modified_date
) VALUES (
    'a1000001-0001-4001-8001-000000000003',
    NULL,
    'C2C qarz da''vo arizasi',
    'C2C иск по займу',
    'C2C loan claim report',
    $REPORT$<?xml version="1.0" encoding="UTF-8"?>
<jasperReport xmlns="http://jasperreports.sourceforge.net/jasperreports"
              name="c2c_debt_report_uz" pageWidth="595" pageHeight="842"
              columnWidth="481" leftMargin="57" rightMargin="57" topMargin="57" bottomMargin="57">
    <parameter name="reportNumber" class="java.lang.String"/>
    <parameter name="number" class="java.lang.String"/>
    <parameter name="sellerFullName" class="java.lang.String"/>
    <parameter name="buyerFullName" class="java.lang.String"/>
    <title>
        <band height="200">
            <staticText>
                <reportElement x="0" y="0" width="481" height="20"/>
                <textElement textAlignment="Center"><font fontName="Times New Roman" size="12" isBold="true"/></textElement>
                <text><![CDATA[DA''VO ARIZASI]]></text>
            </staticText>
            <textField isStretchWithOverflow="true">
                <reportElement x="0" y="30" width="481" height="150"/>
                <textElement textAlignment="Justified"><font fontName="Times New Roman" size="10"/></textElement>
                <textFieldExpression><![CDATA["№ " + $P{reportNumber} + "\n\n" +
$P{sellerFullName} + " va " + $P{buyerFullName} + " o'rtasidagi " + $P{number} +
" raqamli qarz shartnomasi bo'yicha da'vo arizasi."]]]></textFieldExpression>
            </textField>
        </band>
    </title>
</jasperReport>$REPORT$,
    'PUBLISHED',
    'REPORT',
    'a1000001-0001-4001-8001-000000000001',
    NULL,
    NULL,
    FALSE,
    NOW(),
    NOW()
);

COMMIT;

-- Tekshirish
-- SELECT id, name_uz, template_type, template_id, status FROM document.template WHERE id::text LIKE 'a1000001%';
-- SELECT key_name, name_uz FROM document.template_field WHERE template_id = 'a1000001-0001-4001-8001-000000000001';
