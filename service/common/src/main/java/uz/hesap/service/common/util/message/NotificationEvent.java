package uz.hesap.service.common.util.message;

/**
 * Per-template notification config hodisalari. Har bir template uchun har hodisaga alohida
 * SMS/Firebase sozlamasi (integration.template_notification jadvali) bog'lanadi. Document servis bu
 * hodisalarni {@link ContractNotificationEvent} sifatida RabbitMQ'ga e'lon qiladi; integration servis
 * o'z config'iga qarab SMS/push yuboradi.
 */
public enum NotificationEvent {
  DOCUMENT_CREATED, // shartnoma yuborildi (boshqa tarafga)
  CANCEL_REQUEST, // bekor qilish so'rovi
  PAYMENT_REQUEST, // to'lov so'rovi (qarzdor → haqdor)
  PAYMENT_DELAY, // to'lovni kechiktirish so'rovi
  NOTICE, // talabnoma
  CLAIM, // da'vo arizasi
  WITNESS // guvohlik taklifi
}
