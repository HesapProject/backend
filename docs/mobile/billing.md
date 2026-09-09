# Billing — tarif, balans, to‘lov

Base: `/api/billing/v1`

**Auth:** JWT (ko‘pchilik endpointlar)

---

## Tariflar

Base: `/api/billing/v1/tariffs`

```
GET    /api/billing/v1/tariffs/all?type=C2C
GET    /api/billing/v1/tariffs/?type=B2B&page=0&size=20
GET    /api/billing/v1/tariffs/{id}
```

**Response (`TariffResponse`):**
```json
{
  "id": "uuid",
  "name": "Premium",
  "description": { "uz": "...", "ru": "...", "en": "..." },
  "stars": 5,
  "price": 99000,
  "duration": 30,
  "type": "C2C"
}
```

---

## Paketlar

Base: `/api/billing/v1/packages`

```
GET /api/billing/v1/packages/all?type=C2C
GET /api/billing/v1/packages/{id}
```

---

## C2C obuna (CLIENT)

Base: `/api/billing/v1/c2c`

```
POST /api/billing/v1/c2c/package/purchase    { "id": "package-uuid" }
POST /api/billing/v1/c2c/tariffs/purchase    { "id": "tariff-uuid" }
GET  /api/billing/v1/c2c/package/active
```

**Active packages response:**
```json
[
  {
    "balanceId": "uuid",
    "type": "PACKAGE",
    "name": "Starter",
    "price": 49000,
    "stars": 3,
    "duration": 30,
    "expireDate": "2026-04-01",
    "purchasedDate": "2026-03-01",
    "templates": []
  }
]
```

---

## B2B obuna (USER + company)

```
POST /api/billing/v1/tariffs/subscribe       { "tariffId": "uuid" }
POST /api/billing/v1/package/purchase          { "tariffId": "uuid" }
POST /api/billing/v1/application             { "templateApplicationId": "uuid" }
```

**Response:** `BalanceResponse`

---

## Balans

Base: `/api/billing/v1/balance`

```
GET /api/billing/v1/balance/
GET /api/billing/v1/balance/transactions?direction=INCOME&dateFrom=...&dateTo=...&page=0&size=20
GET /api/billing/v1/balance/summary?dateFrom=...&dateTo=...
GET /api/billing/v1/balance/income?page=0&size=20
GET /api/billing/v1/balance/revenue?page=0&size=20
```

B2B — kompaniya balansi, C2C — foydalanuvchi balansi (`UserPrincipal` dan).

---

## C2C tranzaksiya tarixi

Base: `/api/billing/v1/client`

```
GET /api/billing/v1/client/history?startDate=2026-01-01&endDate=2026-12-31
GET /api/billing/v1/client/transactions?startDate=...&endDate=...&page=0&size=20
```

---

## Uzcard — karta bog‘lash

Base: `/api/billing/v1/uzcard`

| Method | Path | Tavsif |
|--------|------|--------|
| POST | `/create` | Karta qo‘shish (OTP keladi) |
| POST | `/confirm` | OTP tasdiqlash |
| GET | `/resend-otp?session=...` | OTP qayta yuborish |
| DELETE | `/delete/{cardId}` | Kartani o‘chirish |
| GET | `/{userId}` | Foydalanuvchi kartalari |

**Create body:**
```json
{
  "userId": "uuid",
  "cardNumber": "8600123456789012",
  "expireDate": "1228",
  "userPhone": "998901234567",
  "pinfl": "12345678901234"
}
```

---

## Payme

```
GET  /api/billing/v1/payme/link?amount=100000          (B2B — kompaniya)
GET  /api/billing/v1/payme/client/link?amount=100000   (C2C — foydalanuvchi)
```

**Response:** `{ "url": "https://checkout.paycom.uz/..." }`

Mobil ilova WebView orqali ochadi.

---

## Click

```
GET /api/billing/v1/click/link?amount=100000          (B2B)
GET /api/billing/v1/click/client/link?amount=100000  (C2C)
```

**Response:** `{ "url": "https://my.click.uz/..." }`

---

## Promo kodlar

```
GET /api/billing/v1/promos/code/{code}
```

---

## Scoring (Plum)

```
POST /api/billing/v1/scoring/create
GET  /api/billing/v1/scoring/{id}
GET  /api/billing/v1/scoring/history/{userId}?page=0&size=20
```

Karta scoring / kredit reytingi.

---

## To‘lov oqimi (mobil)

```
1. GET /payme/client/link?amount=99000
2. WebView da to‘lov
3. Webhook backend ga keladi (mobil kutmaydi)
4. GET /balance/ — balans yangilanganini tekshirish
```
