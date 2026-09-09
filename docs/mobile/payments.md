# Payments — to‘lov jadvallari va operatsiyalar

Base: `/api/document/v1`

**Auth:** JWT

C2C va B2B shartnomalar uchun to‘lov jadvali, so‘rovlar, kechiktirish, to‘langan yozuvlar.

---

## To‘lov jadvali (schedules)

```
GET  /api/document/v1/{documentId}/payment?withFullInfo=true
GET  /api/document/v1/payment/{paymentId}?withFullInfo=true
POST /api/document/v1/{documentId}/payment
DELETE /api/document/v1/payment/{paymentId}
```

**Body (POST — ro‘yxat):**
```json
[
  {
    "amount": 500000,
    "paymentDate": "2026-03-01",
    "status": "PENDING"
  }
]
```

---

## To‘lov so‘rovlari (requests)

```
GET  /api/document/v1/{documentId}/request?withFullInfo=true
GET  /api/document/v1/request/{requestId}?withFullInfo=true
GET  /api/document/v1/payment/{paymentId}/requests?withFullInfo=true
POST /api/document/v1/{documentId}/request
POST /api/document/v1/request/approve          (body: ["uuid", "uuid"])
POST /api/document/v1/request/{requestId}/approve
POST /api/document/v1/request/{requestId}/reject
DELETE /api/document/v1/request/{requestId}
```

---

## Kechiktirish (delay)

```
GET   /api/document/v1/payment/delay?documentId=...&paymentId=...&withFullInfo=true
POST  /api/document/v1/payment/{paymentId}/delay
PATCH /api/document/v1/payment/{paymentId}/delay
```

**Body (POST delay):**
```json
{
  "newPaymentDate": "2026-04-01",
  "reason": "Sabab"
}
```

---

## To‘langan yozuvlar (paid)

```
GET /api/document/v1/{documentId}/paid?withFullInfo=true
GET /api/document/v1/paid/{paidId}?withFullInfo=true
GET /api/document/v1/payment/{paymentId}/paid?withFullInfo=true
PUT /api/document/v1/paid/{paidId}     (status yangilash)
POST /api/document/v1/payment/pay      (to‘lov qilish)
```

**Body (POST pay — `PaymentPaidRequest`):**
```json
{
  "paymentId": "uuid",
  "amount": 500000,
  "paidDate": "2026-03-01"
}
```

---

## To‘lov reytingi (score)

```
GET /api/document/v1/score/{userId}
```

C2C uchun joriy foydalanuvchi: `GET /api/document/v1/c2c/score`

---

## Statuslar

| Status | Ma’nosi |
|--------|---------|
| PENDING | Kutilmoqda |
| PAID | To‘langan |
| OVERDUE | Muddati o‘tgan |
| DELAYED | Kechiktirilgan |

## Direction

| Qiymat | Ma’nosi |
|--------|---------|
| BUYER | Xaridor sifatida |
| SELLER | Sotuvchi sifatida |
