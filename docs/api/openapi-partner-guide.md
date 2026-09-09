# Hesap OpenAPI — hamkorlar uchun ma'lumotnoma

Tashqi tizim (1C, ERP, sayt, mobil ilova) Hesap'da shartnoma tuzishi, holatini kuzatishi
va PDF'ini olishi uchun mo'ljallangan public API.

| | |
|---|---|
| **Base URL** | `https://open.hesap.uz/v1` |
| **Auth** | har bir so'rovda `X-API-Key: hsp_...` headeri |
| **Spec** | `https://open.hesap.uz/api-docs` |

---

## 1. Uch qadamda birinchi shartnoma

**1. Kalit oling.** Business kabinetida **Integratsiyalar** → **Yangi kalit**.
Admin esa Control panelidagi **Integratsiyalar → OpenAPI** kartasidan istalgan mijozga
kalit bera oladi. Kalit matni faqat bir marta ko'rsatiladi.

**2. Shablon id'sini toping.**

```bash
curl https://open.hesap.uz/v1/templates \
  -H "X-API-Key: hsp_..."
```

**3. Shartnoma tuzing.**

```bash
curl -X POST https://open.hesap.uz/v1/contracts \
  -H "X-API-Key: hsp_..." \
  -H "Content-Type: application/json" \
  -d '{
        "templateId": "0f0c1f2e-8a41-4d02-9d55-2f7c1b6f9a30",
        "buyerIn": "51234567890123",
        "price": 1200000000,
        "currency": "UZS"
      }'
```

> **Shartnoma API orqali imzolanmaydi.** Yaratilgan hujjat `CREATED` holatida taraflarni
> kutadi — imzo Hesap ilovasi yoki kabineti orqali qo'yiladi. Ikkala taraf imzolagach
> status `ACTIVE` bo'ladi va `CONTRACT_ACTIVE` webhook yuboriladi.

---

## 2. Kalit va huquqlar

Kalit `hsp_` bilan boshlanadi va **faqat `/openapi/**` yo'llarida** ishlaydi — kabinet
endpointlariga o'tmaydi.

### Huquqlar (scope)

| Scope | Nima beradi |
|-------|-------------|
| `TEMPLATES_READ` | Shablonlar ro'yxati va ularning maydonlari |
| `CONTRACTS_READ` | Shartnomalarni o'qish: ro'yxat, bitta hujjat, PDF |
| `CONTRACTS_WRITE` | Shartnoma tuzish |
| `CLIENTS_READ` | Tarafni PINFL/STIR bo'yicha tekshirish |

### Shablon doirasi

Kalit **barcha shablonlar** bilan ishlashi (full) yoki faqat tanlangan shablonlarga
bog'lanishi mumkin. Ro'yxatda bo'lmagan shablon bo'yicha shartnoma tuzish ham, mavjudini
o'qish ham `403` qaytaradi.

### Muddat

Kalitga aniq tugash sanasi berilishi yoki **abadiy** (muddatsiz) qoldirilishi mumkin.
Muddati o'tgan kalit `401` beradi.

> **Kalit yo'qolsa tiklab bo'lmaydi.** Hesap kalit matnini saqlamaydi — faqat SHA-256
> hash'i saqlanadi. Kabinetdan **Yangilash** (rotate) qiling: yangi kalit beriladi,
> eskisi o'sha zahoti ishlamay qoladi.

---

## 3. Umumiy qoidalar

| Mavzu | Qoida |
|-------|-------|
| **Pul** | Barcha pul qiymatlari **tiyinda** (so'm × 100). 12 000 000 so'm = `1200000000`. Bu `price`, `initialPayment`, to'lov `amount` va mahsulot `price`/`amount` uchun amal qiladi. |
| **Sana** | ISO-8601 UTC: `2026-09-01T00:00:00Z` |
| **Identifikator** | Taraflar `in` bilan: jismoniy shaxsda 14 xonali **PINFL**, yuridikda 9 xonali **STIR**. Foydalanuvchi id'si emas — `in` barqaror. |
| **Sahifalash** | `page` (0 dan), `size` (default 20, maksimum 100). Javob: `content`, `totalElements`, `totalPages`, `number`, `size`. |
| **Format** | `application/json; charset=utf-8` (PDF endpointidan tashqari) |

---

## 4. Endpointlar

| Method | Path | Scope |
|--------|------|-------|
| GET | `/me` | — |
| GET | `/templates` | `TEMPLATES_READ` |
| GET | `/templates/{templateId}/fields` | `TEMPLATES_READ` |
| POST | `/contracts` | `CONTRACTS_WRITE` |
| GET | `/contracts` | `CONTRACTS_READ` |
| GET | `/contracts/{id}` | `CONTRACTS_READ` |
| GET | `/contracts/{id}/pdf` | `CONTRACTS_READ` |
| GET | `/clients/{in}` | `CLIENTS_READ` |

### GET /me

Kalitning joriy holati — integratsiyani sozlashda birinchi chaqiriladigan endpoint.

```json
{
  "name": "1C integratsiyasi",
  "ownerIn": "301234567",
  "ownerLegalName": "OOO ALFA SAVDO",
  "scopes": ["TEMPLATES_READ", "CONTRACTS_READ", "CONTRACTS_WRITE"],
  "allTemplates": false,
  "templateIds": ["0f0c1f2e-8a41-4d02-9d55-2f7c1b6f9a30"],
  "expiresAt": null
}
```

`expiresAt: null` — kalit abadiy.

### GET /templates

Kalit ishlay oladigan, nashr etilgan (`PUBLISHED`) shablonlar.

```json
[
  {
    "id": "0f0c1f2e-8a41-4d02-9d55-2f7c1b6f9a30",
    "nameUz": "Oldi-sotdi shartnomasi",
    "nameRu": "Договор купли-продажи",
    "nameEn": "Sale contract",
    "type": "B2C",
    "status": "PUBLISHED",
    "witnessCount": 0,
    "enabledCurrencies": ["UZS"],
    "productEnabled": true,
    "productRequired": false,
    "paymentScheduleEnabled": true,
    "witnessEnabled": false,
    "initialPaymentEnabled": true
  }
]
```

- `type` — `C2C` / `B2C` / `B2B` / `B2B_SPECIAL`
- `enabledCurrencies` — ruxsat etilgan valyutalar, `null` bo'lsa cheklov yo'q
- `productRequired: true` bo'lsa `products` majburiy

### GET /templates/{templateId}/fields

Shablonning to'ldiriladigan maydonlari — `values` massivini shular bo'yicha yig'asiz.

```json
[
  {
    "id": "7c1d0a55-3b21-4f88-9a0e-1d2b3c4d5e6f",
    "templateId": "0f0c1f2e-8a41-4d02-9d55-2f7c1b6f9a30",
    "parentKey": null,
    "nameUz": "Yetkazib berish manzili",
    "nameRu": "Адрес доставки",
    "nameEn": "Delivery address",
    "keyName": "delivery_address",
    "type": "STRING",
    "position": "BOTTOM",
    "productField": false
  }
]
```

- `productField: true` — maydon mahsulotga tegishli, uni `products[].values` ichiga
  `keyName` kaliti bilan yozing
- `parentKey` — bog'liq maydon kaliti (masalan `SELECT` qiymatiga qarab ochiladigan maydon)

### POST /contracts

| Maydon | Tur | Izoh |
|--------|-----|------|
| `templateId` | uuid | **Majburiy.** Kalitga ruxsat etilgan shablon |
| `buyerIn` | string | Xaridor PINFL/STIR. Bo'sh — kalit egasi |
| `sellerIn` | string | Sotuvchi PINFL/STIR. Bo'sh — kalit egasi |
| `price` | number | Shartnoma summasi, **tiyinda** |
| `currency` | enum | `UZS` / `USD` / `RUB` |
| `initialPayment` | number | Boshlang'ich to'lov, tiyinda |
| `deliveryAt` | instant | Yetkazib berish sanasi |
| `values` | array | Shablon maydonlari qiymatlari |
| `payments` | array | To'lov jadvali: `{ amount, paymentDate }` |
| `products` | array | Mahsulotlar (shablon `productEnabled` bo'lsa) |
| `witnessIds` | array | Guvohlar (foydalanuvchi id'lari) |

> **Taraflardan biri kalit egasi bo'lishi shart.** `buyerIn` yoki `sellerIn` dan biri bo'sh
> qoldirilsa u yerga kalit egasining identifikatori qo'yiladi. Ikkalasi ham begona bo'lsa
> `403` — uchinchi shaxslar nomidan shartnoma tuzib bo'lmaydi.

To'liq so'rov:

```json
{
  "templateId": "0f0c1f2e-8a41-4d02-9d55-2f7c1b6f9a30",
  "buyerIn": "51234567890123",
  "price": 1200000000,
  "currency": "UZS",
  "initialPayment": 200000000,
  "deliveryAt": "2026-09-15T00:00:00Z",
  "values": [
    {
      "templateFieldId": "7c1d0a55-3b21-4f88-9a0e-1d2b3c4d5e6f",
      "keyName": "delivery_address",
      "value": "Toshkent sh., Amir Temur 108",
      "position": 1
    }
  ],
  "payments": [
    { "amount": 500000000, "paymentDate": "2026-10-01T00:00:00Z" },
    { "amount": 500000000, "paymentDate": "2026-11-01T00:00:00Z" }
  ],
  "products": [
    {
      "name": "Sement M400",
      "unit": "KG",
      "price": 120000,
      "quantity": 10000,
      "amount": 1200000000,
      "values": { "brand": "Qizilqum" }
    }
  ]
}
```

Javob:

```json
{ "id": "9b3f7c21-55ad-4e10-8b6e-0c4a91f2d773" }
```

Hujjat raqami (`number`) backendda generatsiya qilinadi, status har doim `CREATED`.

### GET /contracts

Kalit egasi ishtirok etgan shartnomalar — taraf sifatida ham, yaratuvchi sifatida ham.
Kalit shablon bo'yicha cheklangan bo'lsa, ruxsat etilmagan shablon hujjatlari ro'yxatga
tushmaydi.

| Parametr | Tur | Izoh |
|----------|-----|------|
| `statuses` | enum[] | `?statuses=CREATED&statuses=ACTIVE` |
| `templateId` | uuid | Shablon bo'yicha filtr |
| `search` | string | Hujjat raqami yoki taraf bo'yicha qidiruv |
| `page` | int | 0 dan |
| `size` | int | Default 20, maksimum 100 |

### GET /contracts/{id}

```json
{
  "id": "9b3f7c21-55ad-4e10-8b6e-0c4a91f2d773",
  "number": "260815-0042",
  "status": "ACTIVE",
  "purpose": "CONTRACT",
  "buyerStatus": "ACCEPTED",
  "sellerStatus": "ACCEPTED",
  "buyerIn": "51234567890123",
  "sellerIn": "301234567",
  "creatorIn": "301234567",
  "price": 1200000000,
  "currency": "UZS",
  "initialPayment": 200000000,
  "deliveryAt": "2026-09-15T00:00:00Z",
  "templateId": "0f0c1f2e-8a41-4d02-9d55-2f7c1b6f9a30",
  "buyer": {
    "id": "…", "firstName": "Anvar", "lastName": "Qodirov",
    "phone": "998901234567", "legalName": null, "type": "CLIENT"
  },
  "seller": { "…": "…" },
  "template": {
    "id": "…",
    "name": { "uz": "Oldi-sotdi shartnomasi", "ru": "…", "en": "…" },
    "exchangeMode": "GOODS"
  },
  "values": [{ "keyName": "delivery_address", "value": "Toshkent sh." }],
  "products": [{ "name": "Sement M400", "unit": "KG", "amount": 1200000000 }],
  "createdDate": "2026-08-15T09:12:44Z"
}
```

- **Kim imzolaganini `buyerStatus`/`sellerStatus` dan aniqlang**, `status` dan emas
- `creatorIn` — shartnomani kim tuzgan; bekor qilish huquqi shu tarafda
- `values` ro'yxat so'rovida bo'sh keladi, bitta hujjatda to'liq

### GET /contracts/{id}/pdf

`application/pdf` baytlari. `?lang=uz|ru|en` (default `uz`).

```bash
curl https://open.hesap.uz/v1/contracts/9b3f7c21-…/pdf?lang=ru \
  -H "X-API-Key: hsp_..." \
  -o shartnoma.pdf
```

### GET /clients/{in}

Tarafni PINFL yoki STIR bo'yicha tekshirish. Topilmasa `404`.

```json
{
  "in": "301234567",
  "firstName": null,
  "lastName": null,
  "midName": null,
  "legalName": "OOO ALFA SAVDO",
  "type": "COMPANY",
  "verified": true
}
```

`verified` — shaxs MyID (jismoniy) yoki E-IMZO (yuridik) orqali tasdiqlangan.

---

## 5. Webhook

Kalitga `webhookUrl` biriktirilsa, hodisalar shu manzilga `POST` qilinadi. Kabinetda
qaysi hodisalarga obuna bo'lish tanlanadi — hech biri tanlanmasa hammasi keladi.

| Hodisa | Qachon |
|--------|--------|
| `CONTRACT_CREATED` | Shartnoma tuzildi (hali imzolanmagan) |
| `CONTRACT_SIGNED` | Taraflardan biri imzoladi |
| `CONTRACT_ACTIVE` | Ikkala taraf imzoladi — hujjat kuchga kirdi |
| `CONTRACT_REJECTED` | Qarshi taraf rad etdi |
| `CONTRACT_CANCELLED` | Yaratuvchi bekor qildi |
| `PAYMENT_ACCEPTED` | To'lov so'rovi tasdiqlandi |

So'rov tanasi:

```json
{
  "event": "CONTRACT_ACTIVE",
  "contractId": "9b3f7c21-55ad-4e10-8b6e-0c4a91f2d773",
  "contractNumber": "260815-0042",
  "status": "ACTIVE",
  "templateId": "0f0c1f2e-8a41-4d02-9d55-2f7c1b6f9a30",
  "buyerIn": "51234567890123",
  "sellerIn": "301234567",
  "creatorIn": "301234567",
  "actorIn": "51234567890123",
  "amount": 1200000000,
  "currency": "UZS",
  "occurredAt": "2026-08-15T11:04:02Z"
}
```

Headerlar:

| Header | Ma'no |
|--------|-------|
| `X-Hesap-Event` | Hodisa turi |
| `X-Hesap-Delivery` | Yetkazish id'si — takroriy ishlov bermaslik uchun saqlang |
| `X-Hesap-Signature` | `sha256=<hex>` — tananing HMAC-SHA256 imzosi |

Xulq-atvor: timeout 10 s, 3 marta qayta urinish (2 s dan eksponensial), har qanday 2xx
muvaffaqiyat deb qabul qilinadi.

### Imzoni tekshirish

Imzo **xom tana** (raw body) ustidan hisoblanadi — JSON'ni parse qilishdan *oldin*
tekshiring. Secret kalit yaratilganda bir marta beriladi.

```js
const crypto = require("crypto");

function verify(rawBody, signature, secret) {
  const expected = "sha256=" + crypto
    .createHmac("sha256", secret)
    .update(rawBody)
    .digest("hex");

  const a = Buffer.from(expected);
  const b = Buffer.from(signature || "");
  return a.length === b.length && crypto.timingSafeEqual(a, b);
}
```

```php
function verify($rawBody, $signature, $secret) {
    $expected = 'sha256=' . hash_hmac('sha256', $rawBody, $secret);
    return hash_equals($expected, $signature ?? '');
}
```

> **Yetkazib bo'lmagan hodisa yo'qoladi** — uch urinishdan keyin qayta yuborilmaydi.
> Kritik oqimlarda faqat webhook'ga tayanmang, holatlarni vaqti-vaqti bilan
> `GET /contracts` orqali solishtirib turing.

---

## 6. Xatolar

| Kod | Sabab | Nima qilish kerak |
|-----|-------|-------------------|
| 400 | So'rov maydonlari noto'g'ri (masalan `templateId` yo'q, ikkala taraf bo'sh) | Javobdagi `message`ni o'qing |
| 401 | Kalit yuborilmagan, noto'g'ri, bekor qilingan yoki muddati tugagan | Kabinetda kalit holatini tekshiring / rotate qiling |
| 403 | Scope yetishmaydi; shablon ruxsat etilmagan; hujjat kalit egasiga tegishli emas | `GET /me` bilan huquq va shablon doirasini solishtiring |
| 404 | Shartnoma yoki taraf topilmadi | Identifikatorni tekshiring |
| 503 | Ichki servis vaqtincha javob bermadi | Bir necha soniyadan keyin qayta urining |

Xato javobi shakli:

```json
{
  "code": 403,
  "status": "403 Forbidden",
  "path": "/openapi/v1/contracts",
  "message": "Bu shablon kalitga ruxsat etilmagan",
  "description": "Bu shablon kalitga ruxsat etilmagan",
  "timestamp": "15 avgust 2026 y., 16:04:02 UTC+5"
}
```

---

## 7. Ma'lumotnoma (enum qiymatlari)

**Shartnoma holati** — `CREATED`, `ACTIVE`, `COMPLETED`, `REJECTED`, `CANCELLED`
(`SIGNED_BY_BUYER` / `SIGNED_BY_SELLER` eski qiymatlar, yangi hujjatlarda uchramaydi).

**Taraf holati** (`buyerStatus`, `sellerStatus`) — `PENDING`, `ACCEPTED`, `REJECTED`, `CANCELLED`.

**Maydon turi** (`TemplateFieldType`) — `STRING`, `INTEGER`, `DOUBLE`, `DATE`, `LIST`,
`SELECT`, `MULTISELECT`, `CHECKBOX`, `RADIO`, `DOUBLE_INPUT_STRING`, `DOUBLE_INPUT_INTEGER`,
`DOUBLE_INPUT_DOUBLE`, `DOUBLE_INPUT_DATE`.

| Maydon | Qiymatlar |
|--------|-----------|
| `currency` | `UZS`, `USD`, `RUB` |
| `unit` | `DONA`, `KG`, `LITR` |
| `purpose` | `CONTRACT`, `TTN`, `AKT`, `FACTURA` |
| `type` (taraf) | `CLIENT` — jismoniy, `COMPANY` — yuridik |
| `exchangeMode` | `GOODS` — tovar ro'yxati, `MONEY` — pul/qarz |

---

## 8. Ichki tuzilish (Hesap dasturchilari uchun)

- **Kalitlar**: `main` servis, `"user".api_key` jadvali. Kalit matni saqlanmaydi —
  `key_hash` = SHA-256, `key_prefix` faqat UI'da ko'rsatish uchun.
  CRUD: `ApiKeyService` / `ApiKeyController` (`/main/v1/api-keys`).
- **Auth**: `X-API-Key` `document` servisning `JwtConverter`ida ushlanadi va
  `POST /main/v1/local/api-keys/resolve` orqali `UserPrincipal.apiKey` (`ApiKeyContext`) ga
  aylanadi. Yo'l `/openapi/**` bo'lmasa autentifikatsiya o'rnatilmaydi (401).
- **Endpointlar**: `document` servis, `api/openapi` paketi. Scope va shablon tekshiruvi —
  `OpenApiGuard`.
- **Webhook**: `document` → `WebhookEventPublisher` → RabbitMQ (`WebhookEvent`) →
  `integration` servisdagi `WebhookEventConsumer` → `WebhookSender` (HMAC + retry).
  Manzillar `POST /main/v1/local/api-keys/webhooks` orqali olinadi.
- **Spec**: springdoc guruhlari — `document` (ichki) va `openapi` (public).
  Gateway route'lari: `/v3/api-docs/document`, `/v3/api-docs/openapi`, `/openapi/**`.
- **Subdomen**: `open.hesap.uz` alohida servis EMAS — nginx vhost
  (`deploy/nginx/open.hesap.uz.conf`) `/v1/...` ni gateway'ning `/openapi/v1/...`
  yo'liga o'giradi va `/api-docs` ni spec'ga. Public spec'da yo'llardan `/openapi`
  prefiksi olib tashlanadi (`OpenApiDocConfig.applyPublicBaseUrl`), shuning uchun
  spec'dagi manzil hamkor chaqiradigan URL bilan bir xil bo'ladi.
  Eski `api.business.hesap.uz/openapi/v1/...` ham ishlashda davom etadi.
