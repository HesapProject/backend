# Documents C2C — jismoniy shaxslar shartnomalari

Base: `/api/document/v1/c2c`

**Auth:** JWT

---

## Shartnomalar

### Yaratish

```
POST /api/document/v1/c2c/
```

**Body (`C2CDocumentRequest`):**
```json
{
  "sellerId": "uuid",
  "buyerId": "uuid",
  "templateId": "uuid",
  "price": 1000000,
  "currency": "UZS",
  "initialPayment": 200000,
  "deliveryAt": "2026-06-01T00:00:00Z",
  "number": "C2C-001",
  "witnessIds": ["uuid"],
  "values": [{ "fieldId": "uuid", "value": "matn" }],
  "payments": [
    {
      "amount": 500000,
      "paymentDate": "2026-03-01"
    }
  ],
  "products": [
    {
      "productId": "uuid",
      "quantity": 1,
      "price": 1000000
    }
  ]
}
```

### Ro‘yxat

```
GET /api/document/v1/c2c/?page=0&size=20&statuses=DRAFT&statuses=ACTIVE
```

### Bitta shartnoma

```
GET /api/document/v1/c2c/{id}
```

**Response (`C2CDocumentResponse`):**
```json
{
  "id": "uuid",
  "buyer": { "id": "...", "firstName": "...", "phone": "..." },
  "seller": { "id": "...", "firstName": "...", "phone": "..." },
  "number": "C2C-001",
  "status": "ACTIVE",
  "price": 1000000,
  "currency": "UZS",
  "initialPayment": 200000,
  "deliveryAt": "...",
  "witnesses": [],
  "products": [],
  "documentContent": "..."
}
```

### To‘lov reytingi (score)

```
GET /api/document/v1/c2c/score
```

### Kutilayotgan statistika

```
GET /api/document/v1/c2c/pending-stats
```

---

## OTP tasdiqlash

### SMS yuborish

```
POST /api/document/v1/c2c/{docId}/send-verification-sms
```

**Body:**
```json
{ "action": "PARTY_ACCEPT" }
```

**ActionType:** `PARTY_ACCEPT`, `WITNESS_ACCEPT`, va boshqalar.

### Tomon qabul qilish (OTP bilan)

```
POST /api/document/v1/c2c/{docId}/party/accept
POST /api/document/v1/c2c/{docId}/witness/accept
```

**Body:**
```json
{ "code": 1234 }
```

OTP: 4 xonali (1000–9999).

### Rad etish

```
POST /api/document/v1/c2c/{docId}/party/reject
POST /api/document/v1/c2c/{docId}/witness/reject
```

---

## Kutilayotgan to‘lovlar

```
GET /api/document/v1/c2c/pending/payments?role=BUYER&startDate=2026-01-01&endDate=2026-12-31&page=0&size=20
```

**role (`Direction`):** `BUYER`, `SELLER`

---

## Tovarlar yetkazish

```
GET  /api/document/v1/c2c/{docId}/products
POST /api/document/v1/c2c/products/{productId}/sent
POST /api/document/v1/c2c/products/{productId}/received
```

---

## Hamkorlar

```
GET /api/document/v1/partners/
```

Oldingi shartnomalardan hamkorlar ro‘yxati.

---

## Eslatmalar (notices)

```
GET  /api/document/v1/notices/{documentId}?page=0&size=20
POST /api/document/v1/notices/
```

**Body:** `{ "documentId": "uuid" }`

---

## Hisobotlar (reports)

```
GET  /api/document/v1/reports/{documentId}?page=0&size=20
POST /api/document/v1/reports/
```

**Body:** `{ "documentId": "uuid" }`

---

## PDF yuklab olish

```
GET /api/document/v1/generate/{documentId}
```

**Response:** `application/pdf` (binary)

Header: `Authorization: Bearer <token>`
