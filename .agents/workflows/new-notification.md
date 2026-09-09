---
description: Yangi notification turi qo'shish
---

# Yangi Notification Qo'shish

## 1. NotificationType enum ga qo'shish
- Fayl: `service/common/.../util/message/NotificationType.java`
- Yangi enum value qo'sh, unique id bilan
- Category bo'yicha guruhla (PERMISSION 10-19, PASSPORT 20-29, PAYMENT 30-49, DOCUMENT 50-59, REPORT 60+)

## 2. FirebaseNotificationReply ga factory method qo'shish
- Fayl: `service/common/.../util/message/FirebaseNotificationReply.java`
- Static factory method yaratish
- `TextModel` 3 tilda: uz, ru, en
- Tokenlar kerak bo'lsa `of()`, kerak bo'lmasa `withoutTokens()` ishlat

## 3. Service da notification yuborish
- `JmsPublisher` inject qilish (agar yo'q bo'lsa)
- `jmsPublisher.publish(notification)` chaqirish
- `.onErrorResume()` bilan xatoni tutib, log yozish (asosiy flow to'xtamasin)
- `.delayUntil()` orqali async yuborish

## Namuna:
```java
// notification yuborish
FirebaseNotificationReply notification =
    FirebaseNotificationReply.documentCreated(docId, toUserId, docNumber, creatorName);
return jmsPublisher
    .publish(notification)
    .onErrorResume(e -> {
        log.error("Failed to send notification: {}", e.getMessage());
        return Mono.empty();
    });
```
