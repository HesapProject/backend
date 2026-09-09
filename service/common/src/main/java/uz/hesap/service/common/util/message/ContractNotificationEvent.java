package uz.hesap.service.common.util.message;

import java.util.UUID;

/**
 * Kontraktda hodisa sodir bo'lganda document servis RabbitMQ'ga e'lon qiladigan generic xabar.
 * Document matn QURMAYDI — faqat "nima bo'ldi"ni bildiradi. Integration servis bu eventni qabul
 * qilib, o'z template_notification config'iga qarab mijozga SMS/Firebase push yuboradi.
 *
 * <p>Routing-key = klass nomi ("ContractNotificationEvent"); consumer integration servisida.
 *
 * @param event hodisa turi
 * @param templateId shartnoma shabloni id'si (config shu bo'yicha resolve qilinadi)
 * @param documentId shartnoma id'si (push payload uchun)
 * @param docNumber shartnoma raqami ({raqam} placeholder)
 * @param recipientUserId xabar oluvchi foydalanuvchi id'si
 * @param senderName yuboruvchi ismi ({yuboruvchi} placeholder; bo'lishi shart emas)
 */
public record ContractNotificationEvent(
    NotificationEvent event,
    UUID templateId,
    UUID documentId,
    String docNumber,
    UUID recipientUserId,
    String senderName) {}
