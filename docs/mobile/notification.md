# Notification — kontent va bildirishnomalar

Base: `/api/notification/v1`

---

## Public (JWT kerak emas)

### Lead form (landing / marketing)

```
POST /api/notification/v1/lid
```

**Body:**
```json
{
  "phone": "+998901234567",
  "name": "Ism Familiya",
  "businessType": "Savdo"
}
```

Telegram va Google Sheets ga yoziladi.

---

## Protected (JWT kerak)

### FAQ

```
GET /api/notification/v1/faqs
GET /api/notification/v1/faqs/{id}
```

### Blog

```
GET /api/notification/v1/blogs?search=&home=true&page=0&size=20
GET /api/notification/v1/blogs/{id}
```

**Query:**
- `home=true` — bosh sahifa uchun
- `search` — qidiruv

### Stories

```
GET  /api/notification/v1/stories
GET  /api/notification/v1/stories/{id}
GET  /api/notification/v1/stories/user
POST /api/notification/v1/stories/view
```

**View body (`StoryViewRequest`):**
```json
{ "storyId": "uuid" }
```

### Huquqiy hujjatlar

```
GET /api/notification/v1/terms
GET /api/notification/v1/privacy
```

---

## In-app push bildirishnomalar

> **Diqqat:** Path gateway bilan mos kelmaydi!

Controller mapping: `api/notifications/v1/notifications` (ko‘plik `notifications`)

Gateway faqat `/api/notification/**` ni proxy qiladi.

| Method | Path (service ichida) | Gateway orqali? |
|--------|----------------------|-----------------|
| GET | `/api/notifications/v1/notifications` | ❌ Hozircha yo‘q |
| GET | `/api/notifications/v1/notifications/{id}` | ❌ Hozircha yo‘q |

**Mobil integratsiya variantlari:**
1. Backend da path ni `/api/notification/v1/notifications` ga o‘zgartirish (tavsiya)
2. Gateway ga qo‘shimcha route: `/api/notifications/**`
3. Vaqtincha to‘g‘ridan-to‘g‘ri `:8006` port (faqat dev)

Push notificationlar Firebase orqali keladi — mobil ilova `fcmToken` ni login/sign-up da yuboradi.

---

## Response namunalari

### FAQ
```json
{
  "id": "uuid",
  "question": { "uz": "...", "ru": "..." },
  "answer": { "uz": "...", "ru": "..." },
  "order": 1
}
```

### Story
```json
{
  "id": "uuid",
  "title": "...",
  "imageUrl": "https://cdn.../stories/...",
  "videoUrl": "https://cdn.../videos/...",
  "duration": 15,
  "expiresAt": "..."
}
```

### Blog
```json
{
  "id": "uuid",
  "title": "...",
  "content": "...",
  "imageUrl": "...",
  "publishedAt": "..."
}
```

---

## Admin endpointlar (mobil uchun emas)

Quyidagilarni **ishlatmang**:

- `POST/PUT/DELETE /faqs`
- `POST/PUT/DELETE /blogs`, `/blogs/publish`
- `POST/PUT/DELETE /stories`
- `POST /terms`, `POST /privacy`
- `/api/notification/v1/sms/settings/**`
