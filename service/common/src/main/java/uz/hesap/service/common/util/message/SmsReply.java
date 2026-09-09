package uz.hesap.service.common.util.message;

// SMS yuborish so'rovi (async, fire-and-forget) — yuboruvchi servis (user/document)
// → RabbitMQ → integration. integration servis Eskiz orqali haqiqiy yuborishni bajaradi.
// Routing key = klass nomi ("SmsReply"); consumer integration'da.
public record SmsReply(String phone, String message) {}
