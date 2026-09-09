# AI uchun kodlash qoidalari (Coding Conventions)

## 1. SQL da jadval nomlarini Constants orqali ishlatish

Custom repository larda SQL yozganda jadval nomlarini **hech qachon hardcode qilmang**.
Har doim `Constants` klassidagi konstantalardan foydalaning.

### ❌ Noto'g'ri:
```java
String sql = "SELECT * FROM document.document WHERE ...";
```

### ✅ To'g'ri:
```java
import uz.hesap.service.document.util.Constants;

private static final String DOC_TABLE = Constants.SCHEMA + "." + Constants.TABLE_DOCUMENT;

String sql = "SELECT * FROM " + DOC_TABLE + " WHERE ...";
```

### Constants fayli:
```
hesap-business-backend/service/document/src/main/java/uz/hesap/service/**/util/Constants.java
```

Unda barcha jadval nomlari aniqlangan:
- `Constants.SCHEMA` → `"document"`
- `Constants.TABLE_DOCUMENT` → `"document"`
- `Constants.TABLE_TEMPLATE` → `"template"`
- `Constants.TABLE_PAYMENT_SCHEDULE` → `"payment_schedule"`
- va boshqalar...

### Nima uchun?
- Jadval nomi o'zgarsa, **faqat bitta joyda** o'zgartirish kifoya
- SQL da typo qilish xavfi kamayadi
- `@Table(schema = Constants.SCHEMA, name = Constants.TABLE_...)` bilan moslik saqlanadi

## 2. TemplateBasicResponse — common moduldan ishlatish

`TemplateBasicResponse` common modulda mavjud:
```
hesap-business-backend/service/common/src/main/java/uz/hesap/service/common/util/TemplateBasicResponse.java
```

**Import:**
```java
import uz.hesap.service.common.util.TemplateBasicResponse;
```

**Struktura:** `TemplateBasicResponse(UUID id, TextModel name)` — `TextModel(String uz, String ru, String en)`

> ⚠️ Har bir servisda alohida TemplateBasicResponse yaratmang! Common dagisini qayta ishlating.

## 3. Pagination: find va count uchun filterlarni DRY qilish

Sahifalash (pagination) uchun **har doim** ikkita so'rov yoziladi:
1. `SELECT * FROM ... WHERE ... LIMIT/OFFSET` — ma'lumot olish
2. `SELECT COUNT(*) FROM ... WHERE ...` — jami son

Bu ikkala so'rovdagi **WHERE shartlari bir xil** bo'lishi kerak. Agar filterlarni har birida alohida yozsangiz — code duplication va bug xavfi paydo bo'ladi.

### ❌ Noto'g'ri (duplikat filter):
```java
public Flux<Entity> findFiltered(UUID id, Status status, Pageable pageable) {
    StringBuilder sql = new StringBuilder("SELECT * FROM table WHERE deleted = false");
    sql.append(" AND company_id = :id");
    if (status != null) sql.append(" AND status = :status");  // ← duplikat
    // ...
}

public Mono<Long> countFiltered(UUID id, Status status) {
    StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM table WHERE deleted = false");
    sql.append(" AND company_id = :id");
    if (status != null) sql.append(" AND status = :status");  // ← duplikat
    // ...
}
```

### ✅ To'g'ri (umumiy helper):
```java
// WHERE shartlarini qo'shadi
private void appendFilters(StringBuilder sql, UUID id, Status status) {
    sql.append(" WHERE deleted = false");
    sql.append(" AND company_id = :id");
    if (status != null) sql.append(" AND status = :status");
}

// Parametrlarni bog'laydi
private DatabaseClient.GenericExecuteSpec bindFilters(
        DatabaseClient.GenericExecuteSpec spec, UUID id, Status status) {
    spec = spec.bind("id", id);
    if (status != null) spec = spec.bind("status", status.name());
    return spec;
}

public Flux<Entity> findFiltered(UUID id, Status status, Pageable pageable) {
    StringBuilder sql = new StringBuilder("SELECT * FROM ").append(TABLE);
    appendFilters(sql, id, status);
    sql.append(" ORDER BY created_date DESC LIMIT :limit OFFSET :offset");
    var spec = bindFilters(db.sql(sql.toString()), id, status)
            .bind("limit", pageable.getPageSize())
            .bind("offset", pageable.getOffset());
    return spec.map((row, m) -> mapRow(row)).all();
}

public Mono<Long> countFiltered(UUID id, Status status) {
    StringBuilder sql = new StringBuilder("SELECT COUNT(*) AS cnt FROM ").append(TABLE);
    appendFilters(sql, id, status);
    var spec = bindFilters(db.sql(sql.toString()), id, status);
    return spec.map((row, m) -> row.get("cnt", Long.class)).one().defaultIfEmpty(0L);
}
```

### Nima uchun?
- **DRY** — filter mantiq faqat bitta joyda yoziladi
- **Bug oldini olish** — find va count **har doim bir xil** natija qaytaradi
- Yangi filter qo'shsangiz — faqat `appendFilters` + `bindFilters` da o'zgartirish yetarli

## 4. Custom Repository: bir xil mantiqli methodlarni birlashtirish

Agar C2C va B2B uchun query mantigi deyarli bir xil bo'lsa — **alohida methodlar yaratmang**.
Bitta universal method yozing, parametrlar optional bo'lsin.

### ❌ Noto'g'ri (4 ta method — C2C va B2B alohida):
```java
// C2C
Flux<Entity> findAllByUserIdAndStatuses(UUID userId, List<Status> statuses, Pageable p);
Mono<Long> countByUserIdAndStatuses(UUID userId, List<Status> statuses);
// B2B
Flux<Entity> findByCompanyFiltered(UUID companyId, Status status, UUID templateId, Pageable p);
Mono<Long> countByCompanyFiltered(UUID companyId, Status status, UUID templateId);
```

### ✅ To'g'ri (2 ta universal method — barchasi optional):
```java
Flux<Entity> findFiltered(
    UUID userId,        // null → filter qo'shilmaydi
    UUID companyId,     // null → filter qo'shilmaydi
    List<Status> statuses,  // null/empty → filter qo'shilmaydi
    UUID templateId,    // null → filter qo'shilmaydi
    Pageable pageable);

Mono<Long> countFiltered(
    UUID userId, UUID companyId, List<Status> statuses, UUID templateId);
```

### Chaqirish misollari:
```java
// C2C: faqat userId bo'yicha
findFiltered(userId, null, statuses, null, pageable);
countFiltered(userId, null, statuses, null);

// B2B: faqat companyId bo'yicha
findFiltered(null, companyId, List.of(status), templateId, pageable);
countFiltered(null, companyId, List.of(status), templateId);
```

### Nima uchun?
- Bitta `appendFilters` + `bindFilters` — **hech qanday code duplication yo'q**
- Yangi filter qo'shish — faqat 1 joyda
- C2C va B2B bir xil infra ishlatadi

## 5. Umumiy methodlarni alohida Component ga chiqarish (DocumentQueryHelper)

Bir nechta serviceda bir xil mantiq takrorlanayotgan bo'lsa — **har birida private method yozmang**.
Bitta `@Component` yaratib, barcha servicelar uni inject qilsin.

### Mavjud helper:
```
DocumentQueryHelper (service/document/service/document/DocumentQueryHelper.java)
```

### Mavjud methodlar:
| Method | Vazifa |
|--------|--------|
| `findOrThrow(documentId)` | Document topish, topilmasa `NotFoundException` |
| `findAndValidateParty(documentId, userId)` | Document topish + userId buyer/seller ekanligini tekshirish |
| `resolveOppositeParty(document, userId)` | Qarama-qarshi tomonning IDsini qaytaradi |

### ❌ Noto'g'ri (har serviceda duplikat):
```java
// NoticeService.java
private Mono<DocumentEntity> findDocumentOrThrow(UUID docId) { ... }
private Mono<Void> validateParty(UUID userId, UUID buyerId, UUID sellerId) { ... }

// ReportService.java
private Mono<DocumentEntity> findDocumentOrThrow(UUID docId) { ... }  // DUPLIKAT!
private Mono<Void> validateParty(UUID userId, UUID buyerId, UUID sellerId) { ... }  // DUPLIKAT!
```

### ✅ To'g'ri (umumiy component):
```java
@Service
@RequiredArgsConstructor
public class NoticeService {
  private final DocumentQueryHelper documentQueryHelper;

  public Mono<NoticeResponse> create(NoticeCreateRequest request, UUID userId) {
    return documentQueryHelper
        .findAndValidateParty(request.documentId(), userId)
        .flatMap(document -> buildAndSaveNotice(document, userId));
  }
}
```

### Qachon yangi method qo'shish kerak?
Agar 2 yoki undan ko'p serviceda **bir xil mantiq takrorlansa** — `DocumentQueryHelper` ga qo'shing.
Agar faqat bitta service ichida ishlatilsa — private method yetarli.

## 6. Exception throw qilganda **doim ErrorCode** bilan throw qilish

Exception tashlaganda **hech qachon faqat message bilan tashlamang** — `ErrorCode` berish **MAJBURIY**.
`GlobalExceptionHandler` ErrorCode ga qarab `ExceptionResponse.code` ni qaytaradi,
frontend esa shu `code` bo'yicha i18n fayldan foydalanuvchiga tilga mos message ko'rsatadi.

### Arxitektura oqimi:
```
Service → throw NotFoundException(ErrorCode.USER_NOT_FOUND, "log message")
       ↓
GlobalExceptionHandler → e.getCode() → ExceptionResponse(code=1202, message=...)
       ↓
Frontend → code=1202 → i18n["1202"] → "Foydalanuvchi topilmadi"
```

### ❌ Noto'g'ri (ErrorCode yo'q):
```java
throw new NotFoundException("User not found");
// Bu holda default code=NOT_FOUND_ERROR_CODE(1) qaytadi — frontend aniqlay olmaydi
```

### ✅ To'g'ri (ErrorCode bilan):
```java
throw new NotFoundException(ErrorCode.USER_NOT_FOUND, "User not found");
// code=1202 qaytadi — frontend aniq message ko'rsata oladi
```

### Asosiy Exception klasslari va ularning default code lari:
| Exception | Default ErrorCode | HTTP Status |
|-----------|-------------------|-------------|
| `NotFoundException` | `NOT_FOUND_ERROR_CODE(1)` | 404 |
| `ForbiddenException` | `FORBIDDEN_ERROR_CODE(3)` | 403 |
| `InvalidOperationException` | `INVALID_OPERATION_ERROR_CODE(1800)` | 400 |
| `InvalidArgumentException` | `INVALID_ARGUMENT_ERROR_CODE(1500)` | 400 |
| `BadRequestException` | `BAD_REQUEST_CODE(3)` | 400 |
| `AlreadyExistsException` | `ALREADY_EXISTS_ERROR_CODE(1100)` | 400 |
| `UnauthorizedException` | `UNAUTHORIZED_ERROR_CODE(2)` | 401 |
| `ServiceUnavailableException` | `SERVICE_UNAVAILABLE_ERROR_CODE(2)` | 503 |

### Tez-tez ishlatiladigan ErrorCode lar:
```java
ErrorCode.USER_NOT_FOUND          // 1202 — foydalanuvchi topilmadi
ErrorCode.URL_NOT_FOUND           // 1201 — endpoint topilmadi
ErrorCode.REQUIRED_FIELD_MISSED   // 1202 — majburiy maydoni yo'q
ErrorCode.INVALID_OPERATION_ERROR_CODE // 1800 — noto'g'ri operatsiya
ErrorCode.ALREADY_EXISTS_ERROR_CODE    // 1100 — allaqachon mavjud
ErrorCode.FORBIDDEN_ERROR_CODE         // 3 — ruxsat yo'q
```

### Muhim:
- Exception ichidagi `message` — **log uchun** (server tomonda)
-  foydalanuvchiga  — **`code` bo'yicha** i18n dan message ko'rsatadi
- Agar yangi holat kerak bo'lsa — `ErrorCode` enum ga yangi qiymat qo'shing
- `ExceptionInterface.getCode()` orqali `GlobalExceptionHandler` error code ni oladi

### Fayl joylashuvi:
```
service/common/src/main/java/uz/hesap/service/common/exception/handler/ErrorCode.java
service/common/src/main/java/uz/hesap/service/common/exception/handler/ExceptionInterface.java
service/common/src/main/java/uz/hesap/service/common/exception/handler/GlobalExceptionHandler.java
service/common/src/main/java/uz/hesap/service/common/exception/handler/ExceptionResponse.java
```


