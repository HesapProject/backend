# Document Service — Local Context

## Scope
C2C shartnomalar, shablonlar (Template), to'lov jadvallari (PaymentSchedule), 
guvohlar (Witness), hujjat qiymatlari (DocumentValue), mahsulotlar (Product/ContractProduct),
da'vo arizasi (Report), ogohlantirishlar (Notice).

## Schema: `document`

### Key Tables
| Table | Purpose |
|-------|---------|
| `document` | Asosiy shartnoma |
| `template` | Shablon — hujjat tuzilishi |
| `template_field` | Shablon maydonlari |
| `contract_value` | Hujjat maydon qiymatlari (eski `document_value`) |
| `contract_witness` | Guvohlar (PENDING → ACCEPTED/REJECTED) (eski `document_witness`) |
| `contract_payment` | To'lov jadvali (PENDING → PAID) (eski `payment_schedule`) |
| `paid_schedule` | Amalga oshgan to'lovlar |
| `payment_schedule_request` | To'lov/kechiktirish so'rovlari |
| `product` | Mahsulotlar katalogi |
| `contract_product` | Shartnoma mahsulotlari (klonlangan) |

## C2C Flow
1. `C2CCreateDocumentService.create()` — hujjat + values + witnesses + payments parallel saqlaydi
2. `C2CDocumentActionService` — accept/reject (party, witness) + OTP verification
3. Notification har qadamda: witness invite, document created, signed, completed, rejected, cancelled

## Key Patterns
- `DocumentEntity` da `buyerUserId`/`sellerUserId` — C2C uchun companyId null
- `DocumentStatus`: CREATED → SIGNED_BY_BUYER/SELLER → COMPLETED | REJECTED | CANCELLED
- `DocumentWitnessStatus`: PENDING → ACCEPTED | REJECTED
- Template JSON `content` fieldda saqlanadi (hujjat yaratilganda klonlanadi)

## Payment Service Structure

### 3 ta service
- `PaymentScheduleQueryService` — GET (withFullInfo param)
- `PaymentScheduleCommandService` — POST/PUT/DELETE (yangi flow)
- `PaymentScheduleService` — eski backward-compat (`/request/approve`, `/payment/pay`)

### Muhim qoidalar
- **buyer/seller IDlar** requestdan emas, FAQAT `DocumentEntity` dan olinadi (xavfsizlik)
- **withFullInfo:** `false`=faqat IDlar, `true`=user/company to'liq malumoti
- **Reactive:** `throw` emas, `Mono.error()` ishlatish SHART
- **C2C:** company null bo'lishi mumkin — avval companyId, keyin userId tekshir
- **PaidSchedule.amount:** mapper emas, haqiqiy to'langan summa qo'yilsin
- **previousPaymentDate:** delay da eski sana saqlansin
- **Notification:** xatoligi asosiy flowni to'xtatmasin — `onErrorResume`

### Eski controller
- `PaymentController.java` — to'liq commentga olingan
- `PaymentScheduleController.java` — eski URL lar saqlanib + yangilar qo'shilgan

## Testlar
- `PaymentScheduleControllerTest` — `postgres-test` container (localhost:5433) bilan
- `TestSecurityConfig` — testda auth o'chiriladi
- `application-test.yml` — test profil konfiguratsiyasi

## Gotchas
- `PaymentScheduleEntity` da `buyerCompanyId`/`sellerCompanyId` — C2C uchun null
- Custom query uchun `Custom*Repository` pattern — service da `DatabaseClient` ishlatma
- Notification xatoligi asosiy flowni to'xtatmasin — `onErrorResume` bilan qamra
- **Reactive da `throw` ishlatma** — `Mono.error()` ishlat, aks holda 500 qaytadi
- `findById` deleted larni ham qaytaradi — `findByIdAndDeletedFalse` ishlat
- Approve da PENDING tekshiruvi qo'y — CANCELLED/APPROVED qayta approve bo'lmasin
