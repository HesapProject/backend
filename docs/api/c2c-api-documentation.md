# HESAP C2C — To'liq API Hujjatlari

> **Texnologiya:** Java 21, Spring Boot 3.4 (WebFlux), R2DBC, PostgreSQL, RabbitMQ, Firebase  
> **Avtorizatsiya:** Barcha himoyalangan endpointlar `Authorization: Bearer {JWT}` talab qiladi  
> **Base URL format:** `https://{host}/{service-prefix}/v1/...`

---

## 📋 Mundarija

| # | Bo'lim | Service |
|---|--------|---------|
| 1 | [Autentifikatsiya (OneID)](#1-autentifikatsiya-oneid) | User |
| 2 | [Profil boshqaruvi](#2-profil-boshqaruvi) | User |
| 3 | [Sessiya boshqaruvi](#3-sessiya-boshqaruvi) | User |
| 4 | [Permission (Do'stlar) tizimi](#4-permission-dostlar-tizimi) | User |
| 5 | [Ijtimoiy tarmoq ma'lumotlari](#5-ijtimoiy-tarmoq-malumotlari) | User |
| 6 | [Balans va tranzaksiyalar](#6-balans-va-tranzaksiyalar) | Billing |
| 7 | [Paket va tarif sotib olish](#7-paket-va-tarif-sotib-olish) | Billing |
| 8 | [C2C hujjat yaratish va boshqarish](#8-c2c-hujjat-yaratish-va-boshqarish) | Document |
| 9 | [To'lov jadvali (PaymentSchedule)](#9-tolov-jadvali-paymentschedule) | Document |
| 10 | [Partnyorlar](#10-partnyorlar) | Document |
| 11 | [Bildirishnomalar](#11-bildirishnomalar) | Notification |
| 12 | [Blog va yangiliklar](#12-blog-va-yangiliklar) | Notification |

---

## 1. Autentifikatsiya (OneID)

C2C foydalanuvchilari **faqat OneID** orqali tizimga kiradi. Telefon/parol bilan login **o'chirilgan**.

---

### `GET` /api/main/v1/client/one-id/url

**1. Biznes maqsadi:**  
Foydalanuvchini OneID avtorizatsiya sahifasiga yo'naltirish uchun URL generatsiya qiladi. Mobil ilova shu URLni WebView da ochadi.

**2. Ichki ishlash prinsipi:**
- `redirectUrl` parametri olinadi — bu OneID dan qaytish manzili
- OneID API URL quyidagi parametrlar bilan quriladi:
  - `response_type=one_code`
  - `client_id` — `.env` dan olinadi
  - `scope=user_info`
  - `state=client_auth`
- **DB operatsiya:** Yo'q
- **Tashqi chaqiruv:** Yo'q (faqat URL string quriladi)

**3. Request:**
```
GET /api/main/v1/client/one-id/url?redirectUrl=https://app.hesap.uz/callback
```

**4. Response (200 OK):**
```json
{
  "url": "https://sso.egov.uz/sso/oauth/Authorization.do?response_type=one_code&client_id=..."
}
```

---

### `POST` /api/main/v1/client/one-id/verify

**1. Biznes maqsadi:**  
OneID dan qaytgan `code` ni tekshirib, foydalanuvchini tizimga kiritadi. Agar foydalanuvchi birinchi marta kirsa — avtomatik ro'yxatdan o'tkazadi (sign-up + login bir joyda).

**2. Ichki ishlash prinsipi (qadam-baqadam):**

```
1. code + redirectUrl → OneID API: access_token olish
2. access_token → OneID API: foydalanuvchi ma'lumotlari olish (PINFL, ism, familiya, passport)
3. PINFL bo'yicha DB da qidirish:
   a. Topilsa → mavjud user yangilanadi (ism, familiya, passport, tug'ilgan sana)
   b. Topilmasa → yangi UserEntity yaratiladi (type=CLIENT, role=USER)
4. OneID dan kelgan to'liq ma'lumot → one_id_user jadvaliga saqlanadi
5. Device yaratiladi/yangilanadi (uuid, os, model, app versiya)
6. Eski session o'chiriladi (shu deviceId uchun)
7. Yangi SessionEntity yaratiladi (userId + deviceId)
8. 30 kunlik JWT token generatsiya qilinadi
```

- **DB operatsiyalar:** `user`, `one_id_user`, `device`, `session` — UPSERT/INSERT
- **Tashqi chaqiruvlar:** OneID API — 2 ta HTTP POST

**3. Request:**
```json
{
  "code": "abc123",
  "redirectUrl": "https://app.hesap.uz/callback",
  "uuid": "device-uuid-123",
  "os": "android",
  "osVersion": "14",
  "model": "Samsung S24",
  "appVersion": "1.2.0",
  "fcmToken": "firebase-token..."
}
```

**4. Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Xatoliklar:**
| HTTP | Sabab |
|------|-------|
| 401 | OneID `code` noto'g'ri yoki muddati o'tgan |
| 500 | OneID API javob bermadi |

---

## 2. Profil boshqaruvi

### `GET` /api/main/v1/users/me

**1. Biznes maqsadi:** Joriy foydalanuvchining to'liq profilini qaytaradi (ism, passport, rasm, kompaniya, device).

**2. Ichki ishlash:** Token dan `userId` va `deviceId` olinadi → DB dan user + company + device fetch.

**4. Response:** `UserResponse` (id, firstName, lastName, phone, company, image, social, ...)

---

### `PUT` /api/main/v1/users

**1. Biznes maqsadi:** C2C foydalanuvchi o'z profilini tahrirlaydi — **faqat rasm** va **ikkinchi telefon raqam** ni o'zgartira oladi.

**2. Ichki ishlash:**
- `userId` tokendan olinadi — boshqa user ni o'zgartira olmaydi
- `ClientProfileUpdateRequest` da faqat ruxsat berilgan maydonlar bor
- DB: `user` jadvali yangilanadi

**3. Request:**
```json
{
  "image": "https://cdn.hesap.uz/user/photo.jpg",
  "secondPhone": "+998901234567"
}
```

---

### `PUT` /api/main/v1/users/password

**1. Biznes maqsadi:** Parolni o'zgartirish. CLIENT uchun faqat o'z parolini, ADMIN uchun boshqa user parolini ham o'zgartirishi mumkin.

**2. Ichki ishlash:**
- `type == CLIENT` → faqat o'z `userId` olinadi
- `type != CLIENT` va `userId` berilgan → o'sha user uchun
- Eski parol tekshiriladi (`BCrypt.matches`)
- Yangi parol hashlanib saqlanadi

**3. Request:**
```json
{
  "oldPassword": "eski123",
  "newPassword": "yangi456"
}
```

**Xatoliklar:**
| HTTP | Sabab |
|------|-------|
| 400 | Eski parol noto'g'ri |
| 404 | User topilmadi |

---

### `GET` /api/main/v1/users/me/passport

**1. Biznes maqsadi:** OneID dan saqlangan passport ma'lumotlarini qaytaradi (seriya, raqam, berilgan sana, PINFL).

**2. Ichki ishlash:** `one_id_user` jadvalidan `userId` bo'yicha qidiradi → `OneIdPassportResponse` ga map qiladi.

---

### `GET` /api/main/v1/users/pinfl/{pinfl}

**1. Biznes maqsadi:** PINFL bo'yicha foydalanuvchi ID sini qaytaradi. Hujjat yaratishda qarama-qarshi tomonni topish uchun ishlatiladi. **Faqat to'liq 14 xonali PINFL** qabul qilinadi.

**2. Ichki ishlash:** `user` jadvalidan `pinfl` bo'yicha `type=CLIENT` foydalanuvchini qidiradi. Topilmasa — 404.

---

## 3. Sessiya boshqaruvi

### `POST` /api/main/v1/users/logout

**1. Biznes maqsadi:** Joriy qurilmadan chiqish — session va device o'chiriladi.

**2. Ichki ishlash:**
- `deviceId` tokendan olinadi
- `session` jadvalidan `deviceId` bo'yicha session o'chiriladi
- `device` jadvalida `deleted=true` qilinadi

---

### `GET` /api/main/v1/users/sessions

**1. Biznes maqsadi:** Foydalanuvchining barcha aktiv qurilmalarini ro'yxatini ko'rsatadi (OS, model, oxirgi faollik).

**2. Ichki ishlash:** `userId` bo'yicha `session` → `device` JOIN orqali aktiv devicelar qaytariladi.

---

### `DELETE` /api/main/v1/users/sessions/{sessionId}

**1. Biznes maqsadi:** Bitta sessionni masofadan turib o'chirish (boshqa qurilmadan chiqarish).

**2. Ichki ishlash:** `sessionId` bo'yicha session topiladi → faqat o'z sessiyalari uchun ruxsat → session o'chiriladi.

---

## 4. Permission (Do'stlar) tizimi

C2C da bir foydalanuvchi boshqasiga **shartnoma yaratish uchun ruxsat** so'raydi. Bu "do'stlik" sistemasi.

### `POST` /api/main/v1/friends/request

**1. Biznes maqsadi:** Boshqa foydalanuvchiga ruxsat so'rovi (friend request) yuborish. Agar oldin so'rov yuborilgan bo'lsa — yangi yaratmaydi, eskisini yangilaydi.

**2. Ichki ishlash prinsipi:**
```
1. O'ziga o'zi so'rov yuborishni tekshirish → xato
2. Target user mavjudligini tekshirish → 404
3. Mavjud permission bor-yo'qligini tekshirish (user_permission jadvali)
4. Agar PENDING so'rov bor → yangisini yaratmaydi, eskisini yangilaydi
5. Yangi so'rov bo'lsa → PermissionRequestEntity yaratib saqlaydi
6. Target user ga Firebase notification yuboriladi: "Sizga ruxsat so'rovi keldi"
```

- **DB:** `permission_request` INSERT/UPDATE, `user_permission` SELECT
- **Tashqi:** RabbitMQ → Notification service → Firebase

**3. Request:**
```json
{
  "targetUserId": "uuid...",
  "canCreateContract": true,
  "canViewContract": true,
  "canViewPayment": false
}
```

---

### `POST` /api/main/v1/friends/{requestingUserId}/accept

**1. Biznes maqsadi:** Kelgan ruxsat so'rovini qabul qilish. Bu ikki tomonga ham `user_permission` entity yaratadi.

**2. Ichki ishlash:**
```
1. permission_request topiladi (fromUser=requestingUserId, toUser=me, status=PENDING)
2. Status → ACCEPTED ga o'zgaradi
3. user_permission jadvaliga ikki tomonlama yozuv yaratiladi:
   a. fromUser → toUser (so'rov yuboruvchiga ruxsatlar)
   b. toUser → fromUser (qabul qiluvchiga ruxsatlar, request body dan)
4. Notification: "So'rovingiz qabul qilindi"
```

**3. Request:**
```json
{
  "targetUserId": "uuid...",
  "canCreateContract": true,
  "canViewContract": true,
  "canViewPayment": true
}
```

---

### `POST` /api/main/v1/friends/{requestingUserId}/reject

**1. Biznes maqsadi:** Ruxsat so'rovini rad etish.

**2. Ichki ishlash:** `permission_request` topiladi → status → `REJECTED` → notification yuboriladi.

---

### `POST` /api/main/v1/friends/{targetUserId}/cancel

**1. Biznes maqsadi:** O'zi yuborgan ruxsat so'rovini bekor qilish. Shuningdek `user_permission` dan ham o'chiriladi.

**2. Ichki ishlash:**
```
1. permission_request topiladi (fromUser=me, toUser=targetUserId)
2. Status → CANCELLED
3. Agar mavjud bo'lsa user_permission ikki taraflama o'chiriladi (deleted=true)
4. Notification entity o'chiriladi (RabbitMQ orqali)
```

---

### `GET` /api/main/v1/friends/request/user/{toUserId}

**1. Biznes maqsadi:** Belgilangan foydalanuvchiga so'rov yuborilgan-yuborilmaganini tekshirish.

**2. Ichki ishlash:** `permission_request` jadvalidan `fromUser=me, toUser=toUserId` bo'yicha qidiradi.

---

### `GET` /api/main/v1/friends/permissions/{userId}

**1. Biznes maqsadi:** Belgilangan foydalanuvchiga aktiv ruxsat borligini tekshirish (shartnoma yaratishdan oldin).

**2. Ichki ishlash:** `user_permission` jadvalidan `ownerId=me, userId=target` holatni qaytaradi.

---

### `GET` /api/main/v1/friends/request/{id}

**1. Biznes maqsadi:** Notification dan so'rovni ochish uchun — bitta ruxsat so'rovinnig to'liq ma'lumotini qaytaradi.

**2. Ichki ishlash:** ID bo'yicha topib, user ma'lumotlarini enrichment qiladi.

---

### `GET` /api/main/v1/friends/request

**1. Biznes maqsadi:** Menga kelgan (incoming) ruxsat so'rovlari ro'yxati — action kutayotgan so'rovlar.

---

### `GET` /api/main/v1/friends/receive

**1. Biznes maqsadi:** Men yuborgan (outgoing) ruxsat so'rovlari ro'yxati.

---

### `GET` /api/main/v1/friends/followers

**1. Biznes maqsadi:** Men ruxsat **bergan** foydalanuvchilar (mening followerlarim).

---

### `GET` /api/main/v1/friends/follows

**1. Biznes maqsadi:** Menga ruxsat **bergan** foydalanuvchilar (men follow qilganlarim).

---

## 5. Ijtimoiy tarmoq ma'lumotlari

### `POST` /api/main/v1/socials

**1. Biznes maqsadi:** Foydalanuvchi ijtimoiy tarmoq manzillarini saqlash (Telegram, Instagram, va h.k.).

### `GET` /api/main/v1/socials

**1. Biznes maqsadi:** Joriy foydalanuvchining ijtimoiy tarmoq manzillarini qaytaradi.

---

## 6. Balans va tranzaksiyalar

### `GET` /api/billing/v1/balance/summary

**1. Biznes maqsadi:** C2C foydalanuvchining **umumiy moliyaviy holati** — joriy balans, jami kirim, jami chiqim. Dashboard uchun asosiy API.

**2. Ichki ishlash:**
- `userId` tokendan olinadi
- `dateFrom`/`dateTo` — ixtiyoriy sana oraliq filtri
- `balance` jadvalidan joriy qoldiq
- `transaction` jadvalidan SUM(income) va SUM(outcome) hisoblanadi

**3. Request:**
```
GET /api/billing/v1/balance/summary?dateFrom=2026-01-01T00:00:00Z&dateTo=2026-03-01T00:00:00Z
```

**4. Response:**
```json
{
  "balance": 1500000.0,
  "income": 3000000.0,
  "outcome": 1500000.0
}
```

---

### `GET` /api/billing/v1/balance/transactions

**1. Biznes maqsadi:** Foydalanuvchining moliyaviy tranzaksiyalar tarixini sahifalangan holda qaytaradi.

**2. Ichki ishlash:**
- `direction` filtr: `INCOME` | `OUTCOME` | null (barchasi)
- `dateFrom` / `dateTo` — sana oraliq
- `page` / `size` — sahifalash
- B2B uchun `companyId`, C2C uchun `userId` bo'yicha qidiradi

**3. Request:**
```
GET /api/billing/v1/balance/transactions?direction=INCOME&page=0&size=20
```

---

## 7. Paket va tarif sotib olish

### `GET` /api/billing/v1/c2c/package/active

**1. Biznes maqsadi:** Foydalanuvchining **faol paketlarini** ko'rsatadi — qancha shartnoma qolganligini bilish uchun.

**2. Ichki ishlash:** `user_package` jadvalidan `userId` bo'yicha `expired=false` va `remaining > 0` filtri bilan qidiradi. Paket nomi, qolgan miqdor, muddat chiqadi.

---

### `POST` /api/billing/v1/c2c/package/purchase

**1. Biznes maqsadi:** C2C foydalanuvchi **paket sotib oladi** (masalan, "10 ta shartnoma" paketi). Balansdan pul yechildi.

**2. Ichki ishlash:**
```
1. Paket IDsi bo'yicha package jadvalidan topiladi
2. Foydalanuvchi balansivdan paket narxi yechiladi
3. user_package jadvaliga yangi yozuv yaratiladi (remaining = package.count)
4. transaction jadvaliga OUTCOME yozuvi
```

- **DB:** `balance` UPDATE, `user_package` INSERT, `transaction` INSERT
- **@Transactional** — barchasi atomik

**3. Request:**
```json
{
  "id": "package-uuid..."
}
```

**Xatoliklar:**
| HTTP | Sabab |
|------|-------|
| 400 | Balans yetarli emas |
| 404 | Paket topilmadi |

---

### `POST` /api/billing/v1/c2c/tariffs/purchase

**1. Biznes maqsadi:** Tarif sotib olish (paketdan farqi — tarif vaqt asosida, masalan "1 oylik").

---

### `GET` /api/billing/v1/packages/all

**1. Biznes maqsadi:** Mavjud barcha paketlar ro'yxatini qaytaradi (narx, miqdor, tur).

**2. Request:**
```
GET /api/billing/v1/packages/all?type=C2C
```

---

## 8. C2C hujjat yaratish va boshqarish

### `POST` /api/document/v1/c2c

**1. Biznes maqsadi:** Yangi C2C shartnoma yaratish — tizimning asosiy operatsiyasi. Bir vaqtning o'zida hujjat, qiymatlar, guvohlar va to'lov jadvali yaratiladi.

**2. Ichki ishlash prinsipi (qadam-baqadam):**
```
1. TEKSHIRUVLAR:
   a. buyer va seller bir xil bo'lmasligi
   b. Barcha ishtirokchilar (buyer, seller, witnesses) bazada mavjud (user service ga so'rov)
   
2. SHABLON TUZISH:
   a. templateId → template + template_field'lar bazadan olinadi
   b. Template, field'lar va request qiymatlari birlashtiriladi → JSON string
   
3. BILLING TEKSHIRUV:
   a. Billing service ga → paket miqdori kamaytiriladi (decrement)
   b. Agar paket qolmasa → xato qaytadi
   
4. HUJJAT SAQLASH (@Transactional):
   a. DocumentEntity yaratiladi (type=C2C, status=CREATED)
   b. contract_value — maydon qiymatlari saqlanadi
   c. contract_witness — guvohlar saqlanadi (status=PENDING)
   d. contract_payment — to'lov jadvallari yaratiladi (PENDING)
   
5. NOTIFICATION (asinxron):
   a. Har bir guvohga: "Sizni guvohlikka chaqirishyaptilar"
   b. Qarama-qarshi tomonga: "Sizga shartnoma yuborildi"
```

- **DB:** `document`, `contract_value`, `contract_witness`, `contract_payment` — INSERT
- **Tashqi:** User service (user tekshiruv), Billing service (paket kamaytirish), RabbitMQ (notification)

**3. Request:**
```json
{
  "buyerUserId": "uuid...",
  "sellerUserId": "uuid...",
  "templateId": "uuid...",
  "price": 5000000.0,
  "values": [
    { "fieldId": "uuid...", "value": "qiymat" }
  ],
  "witnesses": ["witness-uuid-1", "witness-uuid-2"],
  "payments": [
    { "amount": 2500000.0, "paymentDate": "2026-04-01T00:00:00Z" },
    { "amount": 2500000.0, "paymentDate": "2026-05-01T00:00:00Z" }
  ]
}
```

**4. Response (200):** `C2CDocumentResponse` (id, buyer, seller, number, status, witnesses, content, ...)

**Xatoliklar:**
| HTTP | Sabab |
|------|-------|
| 400 | Buyer = Seller |
| 400 | Paket limit tugagan |
| 404 | User, Template topilmadi |

---

### `POST` /api/document/v1/c2c/{docId}/send-verification-sms

**1. Biznes maqsadi:** Hujjatni imzolash/rad etish uchun SMS OTP kodni yuborish. Har bir harakat (accept/reject) oldidan chaqiriladi.

**2. Ichki ishlash:**
- `action` parametri: `ACCEPT_PARTY`, `ACCEPT_WITNESS`, `REJECT_PARTY`, `REJECT_WITNESS`
- User telefon raqamiga SMS yuboriladi (Eskiz API orqali)
- OTP kod cache da saqlanadi (5 daqiqa muddatli)

---

### `POST` /api/document/v1/c2c/{docId}/party/accept

**1. Biznes maqsadi:** Shartnomaning bir tomoni (buyer/seller) hujjatni **imzolaydi** (tasdiqlaydi). SMS OTP kerak.

**2. Ichki ishlash:**
```
1. OTP kodni tekshirish (verification service) → noto'g'ri bo'lsa xato
2. Hujjatni topish → userId buyer yoki seller ekanligini aniqlash
3. buyer → buyerStatus = ACCEPTED 
   seller → sellerStatus = ACCEPTED
4. IKKALASI ACCEPTED bo'lsa:
   - document.status → COMPLETED
   - Ikkala tomonga: "Shartnoma imzolandi" notification
   - Guvohlar ga: "Shartnoma yakunlandi" notification
5. BITTASI ACCEPTED:
   - Boshqa tomonga: "Shartnoma imzolashingizni kutmoqda" notification
```

- **DB:** `document` UPDATE (status, buyerStatus/sellerStatus)
- **Tashqi:** Verification service (OTP), RabbitMQ (notification)

**3. Request:**
```json
{
  "code": 123456
}
```

---

### `POST` /api/document/v1/c2c/{docId}/party/reject

**1. Biznes maqsadi:** Shartnomani rad etish yoki bekor qilish.

**2. Ichki ishlash:**
```
1. Hujjatni topish
2. Agar hujjat yaratuvchi rad etsa → status = CANCELLED
   Agar boshqa tomon rad etsa → status = REJECTED
3. Barcha ishtirokchilarga notification yuboriladi
```

---

### `POST` /api/document/v1/c2c/{docId}/witness/accept

**1. Biznes maqsadi:** Guvoh hujjatni tasdiqlaydi (SMS OTP bilan).

**2. Ichki ishlash:**
```
1. OTP tekshirish
2. contract_witness jadvalida witnessId bo'yicha topish
3. status → ACCEPTED, acceptedAt = now()
4. Hujjat yaratuvchiga notification: "Guvoh tasdiqladi"
```

---

### `POST` /api/document/v1/c2c/{docId}/witness/reject

**1. Biznes maqsadi:** Guvoh rad etadi (OTP talab qilinmaydi).

**2. Ichki ishlash:** `contract_witness.status → REJECTED` → notification yaratuvchiga.

---

### `GET` /api/document/v1/c2c

**1. Biznes maqsadi:** Foydalanuvchining barcha C2C shartnomalarini ko'rish (buyer yoki seller sifatida).

**2. Ichki ishlash:**
- `userId` tokendan
- `statuses` — ixtiyoriy filtr (CREATED, COMPLETED, REJECTED, ...)
- Har bir hujjat uchun: buyer, seller ma'lumotlari user service dan batch fetch
- Guvohlar `contract_witness` jadvalidan fetch
- `documentJson` → JSON parse qilinadi

**3. Request:**
```
GET /api/document/v1/c2c?statuses=CREATED,COMPLETED&page=0&size=10
```

---

### `GET` /api/document/v1/c2c/{id}

**1. Biznes maqsadi:** Bitta hujjatni to'liq ko'rish — content, buyer, seller, witnesses, status.

**2. Ichki ishlash:**
- Hujjat topiladi → `buyerUserId` yoki `sellerUserId` == me tekshiriladi → aks holda **403 Forbidden**
- User ma'lumotlari, guvohlar, JSON content birgalikda qaytariladi

---

### `GET` /api/document/v1/c2c/score

**1. Biznes maqsadi:** Foydalanuvchining to'lov reytingini ko'rish (o'z vaqtida to'langan/kechiktirilgan shartnomalar statistikasi).

---

### `GET` /api/document/v1/c2c/pending-stats

**1. Biznes maqsadi:** Dashboard uchun — nechta hujjat mening javobimni kutmoqda (buyer va seller sifatida alohida).

**2. Ichki ishlash:** Custom SQL bilan `document` jadvalidan buyer/seller pending count hisoblanadi.

---

### `GET` /api/document/v1/c2c/pending/payments

**1. Biznes maqsadi:** Yaqin kelayotgan to'lovlar ro'yxati — PENDING holatdagi, filterlangan, sahifalangan.

**2. Ichki ishlash:**
- `userId` tokendan olinadi
- **Filtrlar:**
  - `role` — BUYER (qarz) | SELLER (haqdor) | null (barchasi)
  - `startDate` / `endDate` — to'lov sanasi oraliq
- `contract_payment` jadvalidan **faqat PENDING** statusdagilar
- Har bir to'lov uchun: document raqami, buyer/seller ismlari batch fetch

**3. Request:**
```
GET /api/document/v1/c2c/pending/payments?role=BUYER&startDate=2026-03-01T00:00:00Z&page=0&size=20
```

**4. Response:**
```json
{
  "content": [
    {
      "id": "uuid...",
      "documentId": "uuid...",
      "documentNumber": "C2C-2026-001",
      "buyer": { "id": "uuid", "firstName": "Ali", "lastName": "Valiyev" },
      "seller": { "id": "uuid", "firstName": "Vali", "lastName": "Aliyev" },
      "status": "PENDING",
      "amount": 2500000.0,
      "paymentDate": "2026-04-01T00:00:00Z"
    }
  ],
  "totalElements": 5,
  "totalPages": 1
}
```

---

## 9. To'lov jadvali (PaymentSchedule)

> **Muhim:** Barcha GET endpointlarda `?withFullInfo=false` (default). `true` qilsangiz buyer/seller to'liq ma'lumoti qaytadi.

### `GET` /api/document/v1/{documentId}/payment

**1. Biznes maqsadi:** Hujjatdagi barcha to'lov jadvallarini ko'rish.

**2. Ichki ishlash:**
- `withFullInfo=false` → faqat IDlar, user/company fetch qilinmaydi
- `withFullInfo=true` → user service dan batch fetch qilib ism/familiya qo'shiladi

---

### `POST` /api/document/v1/{documentId}/payment

**1. Biznes maqsadi:** To'lov jadvalini yaratish yoki yangilash (toggle pattern). Requestda kelmaganlar soft-delete bo'ladi.

**2. Ichki ishlash:**
```
1. DocumentEntity topiladi → buyerId/sellerId olinadi (request dan EMAS)
2. Mavjud schedule lar fetch
3. Requestda ID bor va DB da topilsa → UPDATE
   Requestda ID yo'q → yangi INSERT
   DB da bor lekin requestda yo'q → soft-delete
4. Barcha o'zgarishlar @Transactional
```

**Xavfsizlik:** `buyerCompanyId`, `sellerCompanyId`, `buyerId`, `sellerId` — faqat DocumentEntity dan olinadi, client spoofing mumkin emas.

---

### `POST` /api/document/v1/payment/pay

**1. Biznes maqsadi:** To'lov amalga oshganligini qayd etish — PaidSchedule yaratiladi.

**2. Ichki ishlash:**
```
1. PaymentSchedule topiladi → PAID bo'lmagan tekshirish
2. Seller to'lasa → status = PAID (to'g'ridan-to'g'ri)
   Buyer to'lasa → status = PENDING (seller tasdiqlashi kerak)
3. Jami to'langan summa hisoblanadi → jadval summasidan oshmasligi tekshiriladi
4. PaidScheduleEntity yaratiladi va saqlanadi
5. Agar to'liq to'langan → PaymentSchedule.status = PAID
6. Seller ga notification: "To'lov qilindi"
```

**3. Request:**
```json
{
  "paymentId": "uuid...",
  "amount": 1500000.0
}
```

---

### `POST` /api/document/v1/payment/{paymentId}/delay

**1. Biznes maqsadi:** Buyer to'lov muddatini kechiktirish so'raydi. Seller tasdiqlashi/rad etishi kerak.

**2. Ichki ishlash:**
- Faqat **buyer** kechiktirish so'rashi mumkin
- Yangi sana mavjud sanadan **keyin** bo'lishi kerak
- `PaymentScheduleRequestEntity` yaratiladi (type=DELAY, status=PENDING)
- Seller ga notification yuboriladi

---

### `PATCH` /api/document/v1/payment/{paymentId}/delay

**1. Biznes maqsadi:** Seller kechiktirishni tasdiqlaydi yoki rad etadi.

**2. Ichki ishlash:**
- Faqat **seller** javob berishi mumkin
- APPROVED → `previousPaymentDate` saqlanadi, `paymentDate` yangilanadi
- CANCELLED → hech narsa o'zgarmaydi
- Buyer ga notification yuboriladi

---

### `PUT` /api/document/v1/paid/{paidId}

**1. Biznes maqsadi:** Seller to'lovni tasdiqlaydi (PAID) yoki rad etadi (CANCELLED).

**2. Ichki ishlash:**
- Faqat **seller** approve/reject qilishi mumkin
- Faqat **PENDING** holatdagi paid lar uchun
- Buyer ga notification yuboriladi

---

## 10. Partnyorlar

### `GET` /api/document/v1/partners

**1. Biznes maqsadi:** Foydalanuvchi shartnoma tuzgan barcha partnyor foydalanuvchilarni ko'rish (har biri bilan nechta hujjat borligini ko'rsatadi).

**2. Ichki ishlash:** `document` jadvalidan GROUP BY buyer/seller orqali partnyorlar va COUNT aniqlanadi → user service dan ism/familiya fetch.

---

## 11. Bildirishnomalar

### `GET` /api/notifications/v1/notifications

**1. Biznes maqsadi:** Foydalanuvchining barcha bildirishnomalarini sahifalab ko'rish (eng yangilari tepada).

**2. Ichki ishlash:**
- `userId` tokendan olinadi
- `notification` jadvalidan `userId` bo'yicha sahifalab qaytariladi
- Sort default: `created_at DESC`
- Har bir notification da: `type`, `title` (3 tillik), `body` (3 tillik), `dataId`, `image`

**3. Request:**
```
GET /api/notifications/v1/notifications?page=0&size=20
```

---

### `GET` /api/notifications/v1/notifications/{id}

**1. Biznes maqsadi:** Bitta bildirishnomani ochish va batafsil ko'rish.

---

## 12. Blog va yangiliklar

### `GET` /api/notification/v1/blogs

**1. Biznes maqsadi:** Barcha blog/yangilik postlarini ko'rish.

**2. Ichki ishlash:**
- `search` — ixtiyoriy qidiruv
- `home` — `true` bo'lsa faqat bosh sahifa uchun bloglar (featured)
- Sahifalash bilan qaytariladi

---

### `GET` /api/notification/v1/blogs/{id}

**1. Biznes maqsadi:** Bitta blog postni to'liq o'qish.

---

### `POST` /api/notification/v1/blogs/publish

**1. Biznes maqsadi:** Blog yaratilganda notification **avtomatik yuborilmaydi**. Ushbu alohida API orqali admin xohlagan vaqtda notification yuboradi. Firebase **topic** (mavzu) bo'yicha barcha foydalanuvchilarga yuboriladi.

**2. Ichki ishlash:**
```
1. Blog ID bo'yicha topiladi
2. Firebase topic ga push notification yuboriladi
3. Blog jadvalida `notificationSent = true` belgilanadi
4. Qayta yuborish mumkin (cheklov yo'q)
```

**3. Request:**
```json
{
  "blogId": "uuid...",
  "topic": "all_users"
}
```

---

## Notification turlarining to'liq ro'yxati

| Tur | Trigger | Kimga | Xabar mazmuni |
|-----|---------|-------|---------------|
| `PERMISSION_REQUEST` | So'rov yaratilganda | Target user | "Sizga ruxsat so'rovi keldi" |
| `PERMISSION_ACCEPTED` | Accept qilganda | So'rovchi | "So'rovingiz qabul qilindi" |
| `PERMISSION_REJECTED` | Reject qilganda | So'rovchi | "So'rovingiz rad etildi" |
| `DOCUMENT_CREATED` | Hujjat yaratilganda | Qarama-qarshi tomon | "Sizga shartnoma yuborildi" |
| `WITNESS_INVITE` | Hujjat yaratilganda | Guvohlar | "Sizni guvohlikka chaqirishyaptilar" |
| `WITNESS_ACCEPTED` | Guvoh tasdiqladi | Yaratuvchi | "Guvoh tasdiqladi" |
| `WITNESS_REJECTED` | Guvoh rad etdi | Yaratuvchi | "Guvoh rad etdi" |
| `PARTY_ACCEPTED` | Tomon imzoladi | Boshqa tomon + guvohlar | "Shartnoma imzolandi" |
| `PARTY_REJECTED` | Tomon rad etdi | Barcha ishtirokchilar | "Shartnoma rad etildi" |
| `DOCUMENT_COMPLETED` | Ikkalasi imzoladi | Barcha | "Shartnoma yakunlandi" |
| `PAYMENT_REQUEST` | To'lov so'rovi | Qarama-qarshi tomonga | "To'lov so'rovi keldi" |
| `PAYMENT_PAID` | To'lov amalga oshdi | Seller | "To'lov qilindi" |
| `PAYMENT_APPROVED` | Seller tasdiqladi | Buyer | "To'lov tasdiqlandi" |
| `PAYMENT_REJECTED` | Seller rad etdi | Buyer | "To'lov rad etildi" |
| `PAYMENT_REQUEST_APPROVED` | Request tasdiqlandi | Buyer | "So'rovingiz tasdiqlandi" |
| `PAYMENT_REQUEST_REJECTED` | Request rad etildi | Buyer | "So'rovingiz rad etildi" |
| `DELAY_REQUEST` | Kechiktirish so'rovi | Seller | "Kechiktirish so'rovi" |
| `DELAY_APPROVED` | Seller tasdiqladi | Buyer | "Kechiktirish tasdiqlandi" |
| `DELAY_REJECTED` | Seller rad etdi | Buyer | "Kechiktirish rad etildi" |
| `NOTICE_CREATED` | Talabnoma yaratildi | Debtor | "Hamkoringiz talabnoma jo'natdi" |
| `REPORT_CREATED` | Da'vo arizasi yaratildi | Debtor | "Hamkoringiz da'vo arizasi jo'natdi" |
| `BLOG_PUBLISHED` | Admin yubordi | Barcha (topic) | Blog sarlavhasi |

---

## Umumiy xatolik kodlari

| HTTP Status | Sabab |
|-------------|-------|
| `400` | Validatsiya xatosi (noto'g'ri payload, mantiqiy xatolik) |
| `401` | Token yo'q yoki muddati o'tgan |
| `403` | Ruxsat yo'q (boshqa foydalanuvchi resursiga kirish) |
| `404` | Resurs topilmadi (hujjat, user, to'lov) |
| `409` | Konflikt (qayta approve, allaqachon to'langan) |
| `500` | Server xatosi |

**Xatolik response formati:**
```json
{
  "code": 3,
  "status": "400 Bad Request",
  "path": "/api/document/v1/c2c",
  "message": "Buyer va seller bir xil bo'lishi mumkin emas",
  "timestamp": "March 5, 2026, 6:05:00 AM UTC+05:00",
  "description": "..."
}
```
