# Friends — do‘stlar va ma’lumot ulashish (C2C)

Base: `/api/main/v1/friends`

**Auth:** JWT (`UserType.CLIENT`)

C2C foydalanuvchilar o‘rtasida passport, to‘lov qobiliyati, shartnoma va hamkor ma’lumotlarini ulashish.

---

## So‘rov yuborish

```
POST /api/main/v1/friends/request
```

**Body:**
```json
{
  "targetUserId": "uuid",
  "userInfo": true,
  "passport": true,
  "payability": false,
  "contract": true,
  "partner": false
}
```

---

## So‘rovni bekor qilish

```
POST /api/main/v1/friends/{targetUserId}/cancel
```

---

## Kiruvchi so‘rovlar

```
GET /api/main/v1/friends/request?page=0&size=20
```

**Response:** `Page<PermissionRequestResponse>`

---

## Chiquvchi so‘rovlar

```
GET /api/main/v1/friends/receive?page=0&size=20
```

---

## Bitta so‘rov

```
GET /api/main/v1/friends/request/{id}
GET /api/main/v1/friends/request/user/{toUserId}
```

---

## Qabul / rad etish

```
POST /api/main/v1/friends/{requestingUserId}/accept
POST /api/main/v1/friends/{requestingUserId}/reject
```

---

## Ulashilgan ruxsatlar

### Men kimga ruxsat berganman

```
GET /api/main/v1/friends/followers?page=0&size=20
```

### Men kimdan ruxsat olganman

```
GET /api/main/v1/friends/follows?page=0&size=20
```

### Aniq foydalanuvchi ruxsati

```
GET /api/main/v1/friends/permissions/{userId}
```

---

## Response struktura

```json
{
  "id": "uuid",
  "userFrom": { "id": "...", "firstName": "...", "phone": "..." },
  "userTo": { "id": "...", "firstName": "...", "phone": "..." },
  "passport": true,
  "payability": false,
  "contract": true,
  "partner": false,
  "status": "PENDING",
  "createdDate": "..."
}
```

**Status:** `PENDING`, `ACCEPTED`, `REJECTED`, `CANCELLED`
