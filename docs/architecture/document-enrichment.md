# B2B Document Service — Data Enrichment

> **Oxirgi yangilanish:** 2026-03-07  
> **O'zgarish:** GET API'lar endi Company va User ma'lumotlari bilan boyitilgan javob qaytaradi

---

## Umumiy tamoyil

Document servisidagi GET endpointlar `DocumentEnrichedResponse` qaytaradi — bu oddiy `DocumentResponse` ga qo'shimcha ravishda buyer/seller uchun to'liq Company va User ma'lumotlarini o'z ichiga oladi.

Ma'lumotlar **batch WebClient** so'rov orqali olinadi — har bir hujjat uchun alohida so'rov emas, barcha IDlar yig'ilib **bitta** so'rov yuboriladi (N+1 muammosi hal qilingan).

---

## Enriched Response Strukturasi

```json
{
  "id": "uuid",
  "buyerCompanyId": "uuid",
  "sellerCompanyId": "uuid",
  "buyerUserId": "uuid",
  "sellerUserId": "uuid",
  "buyerCompany": {
    "id": "uuid",
    "name": "ACME Corp",
    "customName": "ACME",
    "tin": "123456789"
  },
  "sellerCompany": {
    "id": "uuid",
    "name": "Best Trade",
    "customName": null,
    "tin": "987654321"
  },
  "buyer": {
    "id": "uuid",
    "firstName": "Ali",
    "lastName": "Valiyev",
    "phone": "+998901234567"
  },
  "seller": {
    "id": "uuid",
    "firstName": "Vali",
    "lastName": "Aliyev",
    "phone": "+998909876543"
  },
  "templateId": "uuid",
  "number": "DOC-001",
  "status": "COMPLETED",
  "price": 5000000.0,
  "values": [...],
  "deleted": false,
  "createdDate": "2026-03-07T10:00:00Z",
  "lastModifiedDate": "2026-03-07T10:30:00Z",
  "version": 2
}
```

> **Eslatma:** `buyerCompany`, `sellerCompany`, `buyer`, `seller` maydonlari user/company servisda topilmasa `null` qaytadi (hujjat yashab qoladi, foydalanuvchi o'chirilgan bo'lsa ham).

---

## Batch Fetch Arxitekturasi

```
GET /api/document/v1/document
    │
    ▼
DocumentService.getAll(companyId, pageable)
    │
    ├─ 1. documentRepository → List<DocumentEntity>    (1 ta DB query)
    │
    ├─ 2. extractCompanyIds() → Set<UUID>               (memory operation)
    │     extractUserIds()   → Set<UUID>
    │
    ├─ 3. UserServiceClient.getCompaniesByIds(batch)    (1 ta HTTP POST)
    │     UserServiceClient.getUsersBasicInfo(batch)     (1 ta HTTP POST — parallel)
    │
    └─ 4. enrichDocumentList(entities, metadata)         (memory operation)
```

**Natija:** 100 ta hujjat uchun = **1 DB + 2 HTTP** (oldin esa: 1 DB + 200 HTTP)

---

## Tashqi Servis Endpointlari (ichki ishlatish uchun)

| Service | Endpoint | Method | Body | Response |
|---------|----------|:------:|------|----------|
| User | `/api/main/v1/local/users` | POST | `{ "ids": [uuid...] }` | `List<UserBasicResponse>` |
| User | `/api/main/v1/local/companies` | POST | `{ "ids": [uuid...] }` | `List<CompanyBasicResponse>` |
