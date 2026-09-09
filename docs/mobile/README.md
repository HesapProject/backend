# Hesap Mobile API — iOS / Android

Backend microservice tizimi uchun mobil ilova integratsiya hujjatlari.

> **Admin endpointlar kiritilmagan** (`/admin/**`, moderator, backoffice).

## Base URL

| Muhit | URL |
|-------|-----|
| Production | `https://<domain>/` (gateway orqali) |
| Local dev | `http://localhost:8000` |

Barcha API lar **API Gateway** (`:8000`) orqali chaqiriladi. Path prefix o‘zgartirilmaydi.

## Servis marshrutlari

| Prefix | Servis | Port (internal) |
|--------|--------|-----------------|
| `/api/users/**` | User (auth, profil, kompaniya) | 8001 |
| `/api/billing/**` | Billing (tarif, balans, to‘lov) | 8002 |
| `/api/files/**` | File (CDN upload) | 8004 |
| `/api/document/**` | Document (shartnoma, to‘lovlar) | 8005 |
| `/api/notification/**` | Notification (FAQ, blog, story) | 8006 |

## Autentifikatsiya

### JWT

```
Authorization: Bearer <token>
Content-Type: application/json
```

Token `JwtTokenResponse` dan olinadi:

```json
{ "token": "eyJhbGciOiJIUzM4NCJ9..." }
```

Gateway JWT tekshirmaydi — har bir servis o‘zi validatsiya qiladi. Protected endpointlarga **har doim** token yuboring.

### Foydalanuvchi turlari

| `UserType` | Ilova | Asosiy auth |
|------------|-------|-------------|
| `CLIENT` | C2C (jismoniy shaxs) | OneID |
| `USER` | B2B (biznes) | Telefon + parol + SMS OTP |

## Hujjatlar tuzilmasi

| Fayl | Modul |
|------|-------|
| [auth.md](./auth.md) | Ro‘yxatdan o‘tish, login, recovery, OneID, E-Imzo |
| [users-profile.md](./users-profile.md) | Profil, parol, qurilmalar, ijtimoiy tarmoqlar |
| [companies.md](./companies.md) | Kompaniyalar, xodimlar, ruxsatlar (B2B) |
| [friends-permissions.md](./friends-permissions.md) | Do‘stlar, ma’lumot ulashish (C2C) |
| [documents-c2c.md](./documents-c2c.md) | C2C shartnomalar, OTP, tovarlar |
| [documents-b2b.md](./documents-b2b.md) | B2B shartnomalar, shablonlar, mahsulotlar |
| [payments.md](./payments.md) | To‘lov jadvali, so‘rovlar, kechiktirish |
| [billing.md](./billing.md) | Tarif, balans, kartalar, Payme/Click |
| [notification.md](./notification.md) | FAQ, blog, story, push bildirishnomalar |
| [files.md](./files.md) | Rasm/video yuklash (CDN) |

## Umumiy xato kodlari

| HTTP | Ma’nosi |
|------|---------|
| 401 | Token yo‘q yoki muddati tugagan |
| 403 | Ruxsat yetarli emas |
| 404 | Ma’lumot topilmadi |
| 409 | Allaqachon mavjud (masalan, telefon band) |
| 400 | Noto‘g‘ri so‘rov (OTP xato, validatsiya) |

## OpenAPI (Swagger)

Gateway orqali:

- User: `/v3/api-docs/users`
- Document: `/v3/api-docs/document`
- Billing: `/v3/api-docs/billing`
- Notification: `/v3/api-docs/notification`

Swagger UI: `http://localhost:8000/swagger-ui.html`

## Muhim eslatmalar

1. **Telefon formati** — `+998901234567` yoki `998901234567` (backend normalizatsiya qiladi).
2. **Device ma’lumotlari** — login/sign-up/OneID da `uuid`, `os`, `model`, `fcmToken` yuboring (push uchun).
3. **In-app notifications** — path `api/notifications/v1/notifications` (ko‘plik!) gateway `/api/notification/**` ga mos kelmaydi. Hozir to‘g‘ridan-to‘g‘ri servis yoki gateway route qo‘shish kerak bo‘lishi mumkin — batafsil [notification.md](./notification.md).
