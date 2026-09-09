# User Service — Local Context

## Scope
Foydalanuvchi autentifikatsiyasi (JWT+OneID), profil boshqarishi, ruxsatnomalar (Permission),
kompaniyalar, qurilmalar (Device), va PINFL/passport ma'lumotlari.

## Schema: `users`

### Key Tables
| Table | Purpose |
|-------|---------|
| `users` | Foydalanuvchilar (CLIENT, ADMIN, SUPER_ADMIN) |
| `one_id_user` | OneID dan kelgan passport ma'lumotlari (userId PK) |
| `company` | Kompaniyalar |
| `user_company` | User↔Company bog'lanish |
| `permission_request` | Ruxsatnoma so'rovlari |
| `user_permission` | Berilgan ruxsatnomalar |
| `device` | Qurilma tokenlari (Firebase) |

## Auth Flow
1. OneID orqali login → `UserService.auth()` → JWT token qaytaradi
2. `UserType.CLIENT` — jismoniy shaxs (C2C), `UserType.COMPANY` — yuridik shaxs

## Key Patterns
- `PermissionRequestService` — dastlabki PENDING requestni qayta ishlat (duplicate yaratma)
- `expireAllPermissions()` — cron job har kuni soat 3:00 da
- `enrichPermissionRequests()` — batch user fetching (N+1 prevention)

## Exposed Internal APIs (for other services)
```
GET  /api/main/v1/local/users/{id}       → UserResponse
POST /api/main/v1/local/users            → List<UserBasicResponse> (by IDs)
GET  /api/main/v1/local/users/pinfl/{p}  → UserResponse
POST /api/main/v1/local/companies        → List<CompanyBasicResponse> (by IDs)
GET  /api/main/v1/local/companies/inn/{i} → CompanyWithOwner
```
