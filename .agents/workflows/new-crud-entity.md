---
description: Yangi CRUD entity qo'shish uchun workflow
---

# Yangi Entity CRUD qo'shish

## 1. Entity yaratish
- `domain/` papkada `*Entity.java` yaratish
- `@Table(schema = Constants.SCHEMA, name = Constants.TABLE_*)` qo'yish
- `@Id UUID id`, audit fieldlar (`@CreatedDate`, `@LastModifiedDate`), `Boolean deleted = FALSE`
- `Constants.java` ga yangi table nomi qo'shish

## 2. Liquibase migratsiya
- `db.migration/changelog/` da SQL formatida changeset yaratish
- Format: `YYYYMMDD-NN-description.sql`
- Master changelog ga include qo'shish

## 3. Repository
- `repository/` da `*Repository extends R2dbcRepository<*Entity, UUID>` yaratish
- Kerakli finder methodlarni qo'shish (`findAllBy*AndDeletedFalse`)
- Custom query kerak bo'lsa: `Custom*Repository` interface + `Custom*RepositoryImpl` class

## 4. DTOs
- `model/request/` da `*Request` record
- `model/response/` da `*Response` record
- Request va Response alohida bo'lsin (bitta DTO ishlatma)

## 5. Mapper
- `model/mapper/` da `*Mapper` MapStruct interface
- `@Mapper(componentModel = "spring")` + `INSTANCE = Mappers.getMapper()`

## 6. Service
- `service/` da `*Service` class
- `@Service @RequiredArgsConstructor @Log4j2`
- Kichik methodlar, har birida qisqa `//` comment

## 7. Controller
- `api/v1/` da `*Controller` class
- `@RestController @RequiredArgsConstructor @RequestMapping("/api/document/v1/*")`
- `@AuthenticationPrincipal UserPrincipal` orqali userId olish
