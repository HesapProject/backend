# Companies — kompaniyalar va xodimlar (B2B)

**Auth:** JWT (`UserType.USER`)

---

## Kompaniyalar

Base: `/api/main/v1/company`

### Ro‘yxat

```
GET /api/main/v1/company
```

**Response:** `CompanyWithStatus[]`

```json
[
  {
    "id": "uuid",
    "type": "LEGAL",
    "role": "OWNER",
    "status": "ACTIVE",
    "name": "MChJ ...",
    "customName": "Qisqa nom",
    "tin": "123456789",
    "isActive": true
  }
]
```

### OneID orqali kompaniya qo‘shish

```
GET  /api/main/v1/company/one-id/url?redirectUrl=...&customName=...&type=LEGAL
POST /api/main/v1/company/one-id?redirectUrl=...&code=...&customName=...
```

### Kompaniya tahrirlash

```
PUT /api/main/v1/company/{id}
```

**Body (`CompanyRequest`):**
```json
{
  "type": "LEGAL",
  "name": "Kompaniya nomi",
  "tin": "123456789",
  "customName": "Qisqa"
}
```

### Asosiy kompaniyani tanlash

```
POST /api/main/v1/company/main/{companyId}
```

### Foydalanuvchini kompaniyaga qo‘shish

```
POST /api/main/v1/company/user
```

**Body:**
```json
{
  "userId": "uuid",
  "companyId": "uuid",
  "role": "WORKER"
}
```

| Role | Ma’nosi |
|------|---------|
| OWNER | Egasi |
| ADMIN | Administrator |
| WORKER | Xodim |
| USER | Oddiy foydalanuvchi |

### Taklifni qabul qilish

```
POST /api/main/v1/company/user/{id}/accept
```

### Kompaniyadan chiqarish

```
DELETE /api/main/v1/company/user
```

**Body:** `{ "userId", "companyId" }`

---

## Xodimlar

Base: `/api/main/v1/employees`

| Method | Path | Tavsif |
|--------|------|--------|
| GET | `/` | Xodimlar ro‘yxati (pageable) |
| GET | `/me` | Joriy xodim profili |
| POST | `/` | Yangi xodim |
| PUT | `/{id}` | Tahrirlash |
| PATCH | `/{id}/password` | Parol o‘zgartirish |

**Query (GET /):** `search`, `role`, `page`, `size`

**Body (POST):**
```json
{
  "firstName": "Ism",
  "lastName": "Familiya",
  "phone": "+998...",
  "password": "pass123",
  "role": "WORKER"
}
```

---

## Kompaniya ruxsatlari (RBAC)

Base: `/api/main/v1/permission`

```
GET  /api/main/v1/permission/{userId}
POST /api/main/v1/permission
```

**Body:**
```json
{
  "userId": "uuid",
  "permissions": ["CONTRACTS", "PAYMENTS", "EMPLOYEES"]
}
```

**Permission enum:**
`ORGANISATION`, `STATISTICS`, `SETTINGS`, `CHECKUSER`, `NEW_CONTRACT`, `CONTRACTS`, `PAYMENTS`, `EXCHANGES`, `CLIENTS`, `EMPLOYEES`, `TEMPLATES`, `REPORTS`, `BILLING`

---

## Kompaniya foydalanuvchilari

```
GET /api/main/v1/users/company?page=0&size=20
```

**Response:** `Page<UserWithRoleDto>`
