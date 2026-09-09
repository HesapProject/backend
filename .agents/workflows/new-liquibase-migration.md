---
description: Liquibase migratsiya yaratish
---

# Liquibase Migratsiya Yaratish

## 1. Fayl yaratish
- Yo'l: `service/{service-name}/src/main/resources/db.migration/changelog/`
- Nom formati: `YYYYMMDD-NN-description.xml`
- Misol: `20260307-00-add-paid-amount-to-payment-schedule.xml`

## 2. XML formatida `<sql>` tag bilan yozish

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                      http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.9.xsd">

  <changeSet id="YYYYMMDD-NN-description" author="elmurod">
    <sql>
      -- SQL so'rovni shu yerga yoz
    </sql>
  </changeSet>

</databaseChangeLog>
```

## 3. Misollar

### Ustun qo'shish
```xml
<changeSet id="20260307-00-add-paid-amount" author="elmurod">
  <sql>
    ALTER TABLE "document".payment_schedule
    ADD COLUMN IF NOT EXISTS paid_amount DOUBLE PRECISION NOT NULL DEFAULT 0.0;
  </sql>
</changeSet>
```

### Jadval yaratish
```xml
<changeSet id="20260217-02-create-witness-table" author="elmurod">
  <sql>
    CREATE TABLE IF NOT EXISTS "document".document_witness (
        id                     UUID PRIMARY KEY DEFAULT uuid_generate_v4() NOT NULL,
        document_id            UUID NOT NULL,
        status                 VARCHAR(50),
        created_date           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        last_modified_date     TIMESTAMP
    );
  </sql>
</changeSet>
```

### Bir nechta o'zgarish
```xml
<changeSet id="20260217-01-alter-document" author="elmurod">
  <sql>
    ALTER TABLE "document".document
    ADD COLUMN IF NOT EXISTS buyer_status VARCHAR(50),
    ADD COLUMN IF NOT EXISTS seller_status VARCHAR(50),
    ADD COLUMN IF NOT EXISTS type VARCHAR(50);
  </sql>
</changeSet>
```

## 4. Master changelog
- `db.migration/db.changelog-master.yaml` — `includeAll` ishlatadi, alohida qo'shish shart emas
- Fayllar alifbo tartibida avtomatik yuklanadi, shuning uchun **sana bilan nomlash muhim**

## Qoidalar
- ❌ `<addColumn>`, `<createTable>` kabi Liquibase XML tag larni ISHLATMA — faqat `<sql>` tag
- ✅ Har doim `IF NOT EXISTS` / `IF EXISTS` ishlatib, idempotent yoz
- ✅ Yangi ustunlarga doimo `DEFAULT` qiymat ber
- ✅ Har bir mantiqiy o'zgarish — alohida `<changeSet>`
- ✅ Schema nomini qo'shtirnoq bilan yoz: `"document".table_name`
- ❌ Mavjud ustunni o'chirma — `deleted`/deprecated deb belgilagin
- ✅ Index kerak bo'lsa — alohida changeSet da yoz
