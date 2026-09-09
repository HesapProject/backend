# Users — profil va qurilmalar

Base path: `/api/main/v1/users` (B2B) va `/api/main/v1/client` (C2C)

**Auth:** JWT (barcha endpointlar)

---

## Profil

### B2B profil

```
GET /api/main/v1/users/me
```

**Response (`UserResponse`):**
```json
{
  "id": "uuid",
  "firstName": "Ism",
  "lastName": "Familiya",
  "phone": "998901234567",
  "type": "USER",
  "role": "OWNER",
  "verified": true,
  "device": { "id": "...", "uuid": "...", "fcmToken": "..." },
  "company": {
    "id": "uuid",
    "name": "Kompaniya",
    "tin": "123456789"
  },
  "createdDate": "2026-01-01T00:00:00Z"
}
```

### C2C profil

```
GET /api/main/v1/client/me
```

Oddiyroq `UserResponse` (kompaniya yo‘q).

### Profil yangilash (C2C)

```
PUT /api/main/v1/users/
```

**Body:**
```json
{
  "image": "https://cdn.../avatar.jpg",
  "secondPhone": "+998901111111"
}
```

### OneID passport ma’lumotlari

```
GET /api/main/v1/users/me/passport
```

**Response (`OneIdPassportResponse`):**
```json
{
  "userId": "uuid",
  "pin": "AA1234567",
  "fullName": "FAMILIYA ISM",
  "birthDate": "1990-01-01",
  "sex": "M",
  "nationality": "O'zbek",
  "address": "...",
  "photo": "base64..."
}
```

---

## Parol

### Parol bor-yo‘qligini tekshirish

```
GET /api/main/v1/users/is-password
```

**Response:** `true` / `false`

### Parol o‘zgartirish

```
PUT /api/main/v1/users/password
```

**Body:**
```json
{
  "oldPassword": "eski",
  "password": "yangi123"
}
```

---

## Qurilmalar va sessiyalar

```
GET /api/main/v1/users/sessions
```

**Response:** `DeviceResponse[]`

```json
[
  {
    "id": "uuid",
    "uuid": "device-id",
    "os": "iOS",
    "model": "iPhone 15",
    "fcmToken": "...",
    "createdDate": "..."
  }
]
```

### Sessiyani o‘chirish (boshqa qurilmadan chiqish)

```
DELETE /api/main/v1/users/sessions/{sessionId}
```

---

## Foydalanuvchi qidirish (C2C)

```
GET /api/main/v1/client/search?search=Ism
```

**Response:** `UserResponse[]`

---

## PINFL bo‘yicha user ID

```
GET /api/main/v1/users/pinfl/{pinfl}
```

**Response:** `UUID`

---

## Ijtimoiy tarmoqlar

Base: `/api/main/v1/socials`

```
GET  /api/main/v1/socials
POST /api/main/v1/socials
```

**Body (`SocialRequest`):**
```json
{
  "phone2": "+998...",
  "telegram": "@username",
  "instagram": "@username",
  "facebook": "profile-url"
}
```

---

## MyID (identifikatsiya)

Base: `/api/main/v1/myid`

### Session yaratish

```
POST /api/main/v1/myid/session
```

**Body:**
```json
{
  "phone_number": "998901234567",
  "birth_date": "1990-01-01",
  "is_resident": true,
  "pinfl": "12345678901234",
  "threshold": 0.6
}
```

**Response:** `{ "session_id": "..." }`

### Kod verify

```
POST /api/main/v1/myid/code
```

**Body:** `{ "code": "123456" }`

**Response:** `{ "data": { ... }, "reuid": "..." }`
