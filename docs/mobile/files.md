# Files — fayl yuklash (CDN)

Base: `/api/files/v1`

**Auth:** JWT (barcha endpointlar)

---

## CDN upload (tavsiya — mobil uchun)

```
POST /api/files/v1/cdn/upload?type=images
```

**Content-Type:** `multipart/form-data`

| Part | Type | Required |
|------|------|----------|
| file | binary | ha |

**Query `type` (folder):**

| Qiymat | Ma’nosi |
|--------|---------|
| `images` | Rasmlar (avatar, blog) |
| `stories` | Story rasmlar/video |
| `videos` | Video fayllar |

**Response:**
```json
{
  "url": "https://cdn-hesap.fra1.digitaloceanspaces.com/images/abc123.jpg"
}
```

URL **public** — yuklab olish uchun alohida API kerak emas. To‘g‘ridan-to‘g‘ri HTTP GET.

---

## Lokal upload (kam ishlatiladi)

```
POST /api/files/v1/upload
DELETE /api/files/v1/upload?file=/files/userId/uuid.jpg
```

**Response:** `{ "url": "/files/{userId}/{filename}" }`

Bu path infra static server orqali serve qilinadi — gateway orqali emas.

---

## iOS integratsiya

```swift
var request = URLRequest(url: URL(string: "\(baseURL)/api/files/v1/cdn/upload?type=images")!)
request.httpMethod = "POST"
request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")

let boundary = UUID().uuidString
request.setValue("multipart/form-data; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")
// ... multipart body with image data
```

## Android integratsiya

```kotlin
// OkHttp / Retrofit Multipart
@Multipart
@POST("api/files/v1/cdn/upload")
suspend fun upload(
    @Query("type") type: String = "images",
    @Part file: MultipartBody.Part
): UploadResponse
```

---

## Cheklovlar

- Gateway `max-in-memory-size: 16MB` — katta fayllar uchun backend limitini tekshiring
- CDN: DigitalOcean Spaces (`fra1` region)
- Ruxsat etilgan formatlar backend validatsiyasiga bog‘liq

---

## Upload flow

```
1. Foydalanuvchi rasm tanlaydi
2. POST /cdn/upload?type=images (JWT bilan)
3. Response.url ni profil/shartnoma/story API ga yuborish
   masalan: PUT /api/main/v1/users/ { "image": "https://cdn..." }
```
