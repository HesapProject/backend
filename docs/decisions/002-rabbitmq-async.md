# ADR-002: RabbitMQ orqali Async Communication

## Status: Accepted

## Context
Notification yuborish (Firebase push), log yozish kabi operatsiyalar asosiy biznes flowni 
sekinlashtirmasligi kerak. Service-to-service sync call har doim ham zarur emas.

## Decision
- **RabbitMQ** — message broker sifatida
- **JmsPublisher** — barcha servicelar uchun umumiy publisher (`service/jms/` modulda)
- Notification, log kabi operatsiyalar async yuboriladi
- Xatolik bo'lganda `onErrorResume` bilan asosiy flow to'xtamaydi

## Consequences
- Notification/log yo'qolishi mumkin (at-most-once delivery)
- Debug qilish uchun RabbitMQ management UI mavjud (:25672)
- Yangi message type qo'shganda `FirebaseNotificationReply` yoki tegishli Reply class yangilanadi
