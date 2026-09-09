# Hesap Business Backend

## Purpose (WHY)
C2C to'lov va hujjatlarni boshqarish tizimi. Jismoniy shaxslar o'rtasida shartnomalar tuzish, 
to'lov jadvallari, guvohlik, notification va billing bilan ishlash.

## Tech Stack
- **Language**: Java 21 + Spring Boot 3 (WebFlux — reactive)
- **DB**: PostgreSQL + R2DBC (reactive) + Liquibase (migratsiyalar)
- **Messaging**: RabbitMQ (JmsPublisher orqali)
- **Build**: Gradle (Kotlin DSL)
- **Auth**: JWT (UserPrincipal, UserResponse)
- **Mapping**: MapStruct
- **Deploy**: Docker Compose

## Repo Map (WHAT)

```
hesap-business-backend/
├── api-gateway/           # Spring Cloud Gateway — routing va load balancing
├── service/
│   ├── common/            # Shared DTOs, exceptions, utilities (barcha servicelarga dependency)
│   ├── jms/               # RabbitMQ publisher/consumer abstraction
│   ├── main/       :8001  # (eski user) Auth, user CRUD, permissions, OneID, companies, CMS, billing (tarif/balans)
│   ├── log/        :8003  # Audit logs
│   ├── file/       :8004  # CDN, file upload/download
│   ├── document/   :8005  # Shartnomalar, shablonlar, to'lovlar, C2C
│   └── integration/ :8007 # Tashqi integratsiyalar + FCM push + in-app notification + lead
│   # billing(:8002) va notification(:8006) servislari main/integration'ga ko'chirilib o'chirildi
├── docs/                  # Architecture decisions, runbooks
└── docker-compose.yml     # Barcha servicelar + RabbitMQ
```

## Service Communication
- **Sync**: WebClient (service-to-service HTTP calls via `*ServiceClient`)  
- **Async**: RabbitMQ (notifications, logs via `JmsPublisher`)

## Working Rules (HOW)

### Code Conventions
1. **Reactive only** — `Mono`/`Flux`, blocking code yozma
2. **Repository pattern** — DB bilan faqat repository orqali ishla, custom query uchun `Custom*Repository` + `*RepositoryImpl` och
3. **DTO separation** — Request va Response alohida record bo'lsin
4. **MapStruct** — entity↔dto mapping uchun `*Mapper` interface
5. **Kichik methodlar** — har bir method bitta vazifa bajarsn, qisqa `//` comment yoz
6. **Error handling** — `NotFoundException`, `BadRequestException`, `ForbiddenException`
7. **Notification** — `FirebaseNotificationReply` factory methods + `JmsPublisher.publish()`
8. **Liquibase** — migratsiyalar SQL formatida, `db.migration/changelog/` ichida

### Naming
- Entity: `*Entity` (`DocumentEntity`)
- Repository: `*Repository` (`DocumentRepository`)
- Service: `*Service` (`C2CCreateDocumentService`)
- Controller: `*Controller` (`C2CDocumentController`)
- Request DTO: `*Request` (`C2CDocumentRequest`)
- Response DTO: `*Response` (`C2CDocumentResponse`)
- Mapper: `*Mapper` (`TemplateMapper`)

### Database
- Har bir service o'z schemasida (`users`, `document`, `billing`, `notification`, `log`, `file`)
- Table nomlari `Constants.java` da
- Primary key: `UUID`
- Soft delete: `deleted = Boolean.FALSE`
- Audit: `@CreatedDate`, `@LastModifiedDate`

### Do NOT
- `DatabaseClient` ni service ichida ishlatma — Repository/CustomRepository ishlat
- `@Autowired` field injection — constructor injection (`@RequiredArgsConstructor`)
- Blocking calls — `.block()`, `Thread.sleep()` ishlatma
- God methods — bitta method 30+ qator bo'lmasin

## Deployment
- **Faqat bitta environment bor: `prod`.** Dev muhiti 2026-08-15 da yopildi
  (dev branch prod'dan oylab orqada qolar, dev DB alohida liniya edi — u yerdagi
  sinov prod holatini aks ettirmasdi).
- **Git branch: `prod`.** `master`/`main`/`dev` branch YO'Q — ularga murojaat qilma.
- Ish tartibi: `feat/...` yoki `fix/...` branch → MR → `prod` → CI deploy.
- Prodga chiqarishdan oldin lokal `./gradlew build` bilan tekshir; yangi Liquibase
  changeset yozsang, uni idempotent qil (`IF NOT EXISTS`) — orqaga qaytarish yo'li yo'q.
