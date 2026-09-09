# Integration Service — Local Context

## Scope
Tashqi integratsiyalar (Eskiz SMS, Telegram, Google Sheets, Firebase va boshqalar).
Faqat integration logikasi uchun alohida servis.

## Schema: `integration`

## Port
`8007` — `/api/integration/**`

## FCM push (notification servisidan ko'chirildi)
- `FirebaseProvider` — yagona FCM yuborish joyi (user multicast + topic). Bloklovchi
  `FirebaseMessaging` chaqiruvlari `Schedulers.boundedElastic()` da.
- `FirebaseConfig` — `firebase.json` (classpath resource) credential.
- Endpointlar (s2s, `/local/**` permitAll): `POST /local/firebase/send-user`
  (`FirebaseNotificationReply` — token yo'q bo'lsa `UserServiceClient.getFirebaseTokens`),
  `POST /local/firebase/send-topic` (`FirebaseTopicReply`).
- `USER_SERVICE` env — token resolve uchun.
