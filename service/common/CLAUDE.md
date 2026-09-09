# Common Module — Local Context

## Scope
Barcha servicelarga umumiy DTOs, exceptions, va utility classlar.
Bu modul **hech qanday service ga dependency bo'lmaydi** — faqat boshqa servicelar bunga depend qiladi.

## Key Classes

### Exceptions (`exception/`)
| Class | HTTP Status |
|-------|-------------|
| `NotFoundException` | 404 |
| `BadRequestException` | 400 |
| `ForbiddenException` | 403 |
| `AlreadyExistsException` | 409 |
| `InvalidOperationException` | 422 |

### DTOs (`util/`)
| Class | Purpose |
|-------|---------|
| `UserResponse` | Toliq user info (id, fullName, phone) |
| `UserBasicResponse` | Qisqa user info (id, firstName, lastName, phone) |
| `UserPrincipal` | JWT dan olingan auth context |
| `CompanyBasicResponse` | Kompaniya qisqa info |
| `CompanyWithOwner` | Kompaniya + egasi |
| `TextModel` | Ko'p tilli matn (uz, ru, en) |

### Notification (`util/message/`)
| Class | Purpose |
|-------|---------|
| `FirebaseNotificationReply` | Notification yaratish uchun factory methods |
| `NotificationType` | Notification turlari enum |
| `CancelNotificationReply` | Notificationni bekor qilish |

## Rules
- Bu modulga yangi exception qo'shsang — `@ResponseStatus` anotatsiyasi qo'y
- `FirebaseNotificationReply` ga yangi notification turi qo'shsang — `NotificationType` enum ga ham qo'sh
- `TextModel` — doimo 3 til (uz, ru, en)
