# Auth — autentifikatsiya

Base path: `/api/main/v1`

## B2B — telefon + parol (`UserType.USER`)

### 1. Ro‘yxatdan o‘tish (2 bosqich)

#### 1-qadam: SMS OTP yuborish

```
POST /api/main/v1/users/sign-up
```

**Auth:** yo‘q (public)

**Body:**
```json
{
  "phone": "+998901234567",
  "password": "MyPassword123",
  "firstName": "Ism",
  "lastName": "Familiya"
}
```

| Field | Type | Required |
|-------|------|----------|
| phone | string | ha |
| password | string | ha |
| firstName | string | yo‘q |
| lastName | string | yo‘q |

**Response:** `204 No Content`

SMS orqali 5 xonali OTP keladi.

#### 2-qadam: OTP tasdiqlash + token olish

```
POST /api/main/v1/users/sign-up/confirm
```

**Body:**
```json
{
  "phone": "+998901234567",
  "password": "MyPassword123",
  "code": "12345",
  "uuid": "device-unique-id",
  "osVersion": "17.0",
  "os": "iOS",
  "model": "iPhone 15",
  "brand": "Apple",
  "type": "mobile",
  "device": "iPhone",
  "fcmToken": "firebase-token"
}
```

| Field | Type | Required |
|-------|------|----------|
| phone | string | ha |
| password | string | ha (sign-up dagi parol ishlatiladi) |
| code | string | ha (SMS dagi OTP) |
| uuid | string | ha (qurilma ID) |
| fcmToken | string | tavsiya |

**Response:**
```json
{ "token": "eyJ..." }
```

---

### 2. Login

```
POST /api/main/v1/users/login
```

**Body:** `LoginRequest` (sign-up/confirm bilan bir xil, `code` kerak emas)

**Response:** `{ "token": "..." }`

---

### 3. Parol tiklash (3 bosqich)

| Qadam | Method | Path | Body |
|-------|--------|------|------|
| 1 | POST | `/users/recovery` | `{ "phone" }` |
| 2 | POST | `/users/recovery/confirm` | `{ "phone", "code" }` |
| 3 | POST | `/users/recovery/password` | `{ "phone", "password" }` |

---

### 4. Telefon mavjudligini tekshirish

```
POST /api/main/v1/users/check
```

**Body:** `{ "phone": "+998..." }`

---

### 5. Logout

```
POST /api/main/v1/users/logout
```

**Auth:** JWT

---

## C2C — OneID (`UserType.CLIENT`)

Base path: `/api/main/v1/client`

### 1. OneID URL olish

```
GET /api/main/v1/client/one-id/url?redirectUrl=<callback_url>
```

**Auth:** yo‘q

**Response:**
```json
{ "url": "https://sso.egov.uz/..." }
```

Mobil ilova WebView yoki browser orqali ochadi.

### 2. OneID code ni verify qilish

```
POST /api/main/v1/client/one-id/verify
```

**Body:**
```json
{
  "code": "oneid-auth-code",
  "redirectUrl": "hesap://callback",
  "uuid": "device-id",
  "osVersion": "17.0",
  "os": "iOS",
  "model": "iPhone 15",
  "brand": "Apple",
  "type": "mobile",
  "device": "iPhone",
  "fcmToken": "firebase-token"
}
```

**Response:** `{ "token": "..." }`

---

## E-Imzo (elektron imzo)

```
POST /api/main/v1/e-imzo/auth
```

**Auth:** yo‘q

**Body:**
```json
{
  "uuid": "device-id",
  "pkcs7": "<base64-pkcs7>",
  "os": "Android",
  "fcmToken": "..."
}
```

**Response:** `{ "token": "..." }`

---

## Xodim login (B2B sub-account)

```
POST /api/main/v1/employees/login
```

**Auth:** yo‘q

**Body:** `LoginRequest`

**Response:** `{ "token": "..." }`

---

## Auth flow diagrammalari

### B2B sign-up

```
Mobile                    API
  |-- POST /sign-up ------>|
  |<-- 204 (SMS sent) -----|
  |-- POST /sign-up/confirm ->|
  |<-- { token } ----------|
  |  (keyingi so'rovlar: Authorization: Bearer token)
```

### C2C OneID

```
Mobile                    API                    OneID SSO
  |-- GET /one-id/url ---->|
  |<-- { url } ------------|
  |-- open WebView ------------------------->|
  |<-- redirect with code -------------------|
  |-- POST /one-id/verify ->|
  |<-- { token } ----------|
```

## Xatolar

| Xabar | Sabab |
|-------|-------|
| `User code not match` | OTP noto‘g‘ri yoki muddati tugagan |
| `phone or email already exists` | Telefon band |
| `phone not found` | Telefon ro‘yxatda yo‘q |
| `Verification code expired or not found` | Cache da OTP yo‘q (servis qayta ishga tushgan) |
