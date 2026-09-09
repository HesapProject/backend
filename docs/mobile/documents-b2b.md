# Documents B2B — biznes shartnomalari

Base: `/api/document/v1`

**Auth:** JWT (kompaniya konteksti)

---

## Shartnomalar

Base: `/api/document/v1/document`

| Method | Path | Tavsif |
|--------|------|--------|
| GET | `/` | Ro‘yxat (pageable) |
| GET | `/{id}` | Bitta shartnoma |
| POST | `/` | Yaratish |
| PUT | `/{id}` | Tahrirlash |
| DELETE | `/{id}` | O‘chirish |
| POST | `/sign` | Imzolash |
| POST | `/e-imzo/timestamp` | E-Imzo timestamp |

**Query (GET /):** `status`, `templateId`, `withValues`, `page`, `size`, `sort`

**Body (`DocumentRequest`):**
```json
{
  "buyerCompanyId": "uuid",
  "sellerCompanyId": "uuid",
  "buyerUserId": "uuid",
  "sellerUserId": "uuid",
  "templateId": "uuid",
  "number": "DOG-001",
  "status": "DRAFT",
  "price": 5000000,
  "currency": "UZS",
  "initialPayment": 1000000,
  "deliveryAt": "2026-06-01T00:00:00Z",
  "values": [{ "fieldId": "uuid", "value": "..." }],
  "products": []
}
```

**Imzolash (`SignDocumentRequest`):**
```json
{
  "documentId": "uuid",
  "pkcs7": "<base64>"
}
```

---

## Shablonlar

Base: `/api/document/v1/template`

```
GET    /api/document/v1/template?type=CONTRACT
GET    /api/document/v1/template/{id}
POST   /api/document/v1/template/
PUT    /api/document/v1/template/{id}
DELETE /api/document/v1/template/{id}
```

**TemplateType:** shartnoma turi (backend enum).

### Shablon maydonlari

```
GET    /api/document/v1/template/field/{templateId}
POST   /api/document/v1/template/field/{templateId}
DELETE /api/document/v1/template/field/{id}
```

---

## Shablon arizalari

Base: `/api/document/v1/applications`

```
GET  /api/document/v1/applications/?fromDate=...&toDate=...&page=0&size=20
POST /api/document/v1/applications/
```

---

## Mahsulot katalogi

Base: `/api/document/v1/product`

```
GET    /api/document/v1/product/?name=...&page=0&size=20
GET    /api/document/v1/product/{id}
POST   /api/document/v1/product/
PUT    /api/document/v1/product/{id}
DELETE /api/document/v1/product/{id}
```

---

## Shartnoma mahsulotlari

```
GET /api/document/v1/document/products/{contractId}
GET /api/document/v1/document/products/stats?direction=...&statuses=...&page=0&size=20
```

---

## B2B to‘lovlar overview

Base: `/api/document/v1/payment`

```
GET /api/document/v1/payment/schedules?direction=...&statuses=...&startDate=...&endDate=...&page=0&size=20
GET /api/document/v1/payment/paid?direction=...&page=0&size=20
GET /api/document/v1/payment/stats
GET /api/document/v1/payment/products?page=0&size=20
```

---

## PDF

```
GET /api/document/v1/generate/{documentId}
```

**Response:** PDF binary
