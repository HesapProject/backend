# Eski Hesap → yangi backend: user migratsiyasi

Eski Ktor backend DB'sidagi (`trust`, jadval: `public.users`) barcha userlarni
yangi mikroservis DB'sidagi (`hesap`) `"user"."user"` jadvaliga ko'chiradi.

## Mapping

| Eski (`trust.users`) | Yangi (`"user"."user"`) | Izoh |
|---|---|---|
| `id` (bigint) | `id` (UUID) | `uuid_generate_v5(uuid_ns_url(), 'hesap:old-user:<id>')` — deterministik |
| `username` | `username` | lowercase; aktiv userda band bo'lsa NULL |
| `firstname/lastname/middlename` | `first_name/last_name/mid_name` | |
| `phone` | `phone` | normalizatsiya: faqat raqamlar (`+`, ` `, `-` olib tashlanadi) |
| `pnfl` | `pinfl` | |
| `passport_number` | `passport` | |
| `type` | `CLIENT` | eski DB'da faqat jismoniy shaxslar — hammasi CLIENT |
| `tin` yoki `inn` | `tin` | bo'lsa olinadi, odatda bo'sh |
| `verified` | `is_verified` | |
| `is_deleted` | `deleted` | o'chirilganlar ham ko'chiriladi (FK butunligi uchun) |
| `created_at` | `created_date` | |
| `socials` jadvali | `"user".socials` + `second_phone` | |
| `password` | — | yangi tizimda parol yo'q (OneID/SMS auth), ko'chirilmaydi |

Eski `id` ↔ yangi `UUID` xaritasi **`migration.user_id_map`** jadvalida qoladi —
keyingi bosqichlar (shartnomalar, billing, partner va h.k.) shu orqali bog'lanadi.

Telefoni bo'yicha yangi DB'da allaqachon ro'yxatdan o'tgan CLIENT bor bo'lsa,
duplikat yaratilmaydi — eski id mavjud userga bog'lanadi (`matched_existing = TRUE`).

## Ishga tushirish

Ikkala DB'ga ham ulanish kerak (server ichida yoki SSH tunnel orqali).
Eski DB: `localhost:15432`, db=`trust`, user=`admin` (parol `config_prod.json` da).

```bash
cd api/scripts/migration

# 1. Eski DB'dan export (CSV)
OLD_DB_URL="postgresql://admin:CHANGE_ME@localhost:15432/trust" ./01-export-old-users.sh ./old-data

# 2. Yangi DB'ga import (CSV papkadan turib!)
cd old-data
psql "postgresql://postgres:CHANGE_ME@localhost:5435/hesap" -f ../02-import-old-users.sql
```

Import skripti idempotent — qayta ishga tushirish xavfsiz. Oxirida hisobot chiqadi:
nechta ko'chirildi, nechta mavjudga bog'landi, telefon dublikatlari ro'yxati.

## Tekshirish

```sql
-- son tengligi
SELECT count(*) FROM migration.old_users;
SELECT count(*) FROM migration.user_id_map;

-- namuna solishtirish
SELECT o.id, o.phone, o.firstname, u.id, u.phone, u.first_name
FROM migration.old_users o
JOIN migration.user_id_map m ON m.old_id = o.id
JOIN "user"."user" u ON u.id = m.new_id
LIMIT 20;
```

## Keyingi bosqichlar (bu skript qamrab olmaydi)

- Passport rasmlari / MyID profillari (`passport` jadvali → `user.myid_profile`)
- Shartnomalar (`contract` → `document.document`) — `migration.user_id_map` dan foydalaniladi
- Balans/to'lovlar (`billing.balance`, `billing.transaction`)
- Device/FCM tokenlar (push xabar uzilmasligi uchun)
