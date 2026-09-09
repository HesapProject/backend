# ADR-003: Microservice Arxitektura va Service Separation

## Status: Accepted

## Context
Loyiha bir nechta mustaqil business domainlardan iborat: user management, billing, 
document management, notifications, file storage. Ular alohida rivojlantirish, 
deploy, va scale qilish imkonini berishi kerak.

## Decision
Har bir domain alohida Spring Boot application:
- **user** — autentifikatsiya, profil, ruxsatnomalar
- **billing** — tariflar, subscription
- **document** — shartnomalar, to'lovlar, C2C
- **notification** — Firebase push, in-app
- **file** — CDN, upload/download
- **log** — audit

Umumiy kod `common` modulda, message brokerlik esa `jms` modulda saqlanadi.

## Communication
- **Sync**: `WebClient` orqali internal API chaqirish (`*ServiceClient`)
- **Async**: `JmsPublisher` orqali RabbitMQ ga message yuborish

## Database
Har bir service o'z PostgreSQL schemasida ishlaydi:
- `users`, `billing`, `document`, `notification`, `file`, `log`
- Cross-schema query **taqiqlanadi** — faqat service API orqali

## Consequences
- Service o'rtasida ma'lumot olish uchun HTTP call kerak (network latency)
- Shared DTO lar `common` modulda — o'zgartirish barcha servicelarga ta'sir qiladi
- Deploy alohida — bitta service buzilsa boshqalarga ta'sir qilmaydi
