package uz.hesap.service.common.util.message;

import java.util.List;
import java.util.UUID;
import uz.hesap.service.common.util.TextModel;
import uz.hesap.service.common.util.UserResponse;

public record FirebaseNotificationReply(
    UUID dataId,
    TextModel title,
    TextModel body,
    NotificationType type,
    List<String> tokens,
    UUID toUserId,
    Boolean tokensDeferred) {

  // tokens oldindan mavjud bo'lganda
  public static FirebaseNotificationReply of(
      UUID dataId,
      TextModel title,
      TextModel body,
      NotificationType type,
      List<String> tokens,
      UUID toUserId) {
    return new FirebaseNotificationReply(dataId, title, body, type, tokens, toUserId, null);
  }

  // tokens yo'q — notification service o'zi oladi
  public static FirebaseNotificationReply withoutToken(
      UUID dataId, TextModel title, TextModel body, NotificationType type, UUID toUserId) {
    return new FirebaseNotificationReply(dataId, title, body, type, null, toUserId, Boolean.TRUE);
  }

  // ======================== PERMISSION ========================

  public static FirebaseNotificationReply permission(
      UUID requestId, UserResponse toUser, UserResponse fromUser, List<String> tokens) {
    return new FirebaseNotificationReply(
        requestId,
        new TextModel(
            "Sizning ma'lumotlaringiz so'ralmoqda",
            "Запрашиваются ваши данные",
            "Your personal data is requested"),
        new TextModel(
            String.format("%s ma'lumotlaringizni so'rayabdi", fromUser.fullName()),
            String.format("%s запрашиваются ваши данные", fromUser.fullName()),
            String.format("%s is requesting your personal info", fromUser.fullName())),
        NotificationType.PERMISSION_REQUEST,
        tokens,
        toUser.id(),
        null);
  }

  public static FirebaseNotificationReply permissionAccepted(
      UUID requestId, UserResponse toUser, UUID fromUserId, List<String> tokens) {
    return new FirebaseNotificationReply(
        requestId,
        new TextModel("Ruxsatnoma tasdiqlandi", "Разрешение подтверждено", "Permission granted"),
        new TextModel(
            String.format("%s sizga ma'lumotlarini ko'rishga ruxsat berdi", toUser.fullName()),
            String.format("%s предоставил вам доступ к своим данным", toUser.fullName()),
            String.format("%s granted you access to their personal info", toUser.fullName())),
        NotificationType.PERMISSION_ACCEPTED,
        tokens,
        fromUserId,
        null);
  }

  public static FirebaseNotificationReply permissionRejected(
      UUID requestId, UserResponse toUser, UUID fromUser, List<String> tokens) {
    return new FirebaseNotificationReply(
        requestId,
        new TextModel("Ruxsatnoma rad etildi", "Разрешение отклонено", "Permission rejected"),
        new TextModel(
            String.format("%s sizning so'rovingizni rad etdi", toUser.fullName()),
            String.format("%s отклонил ваш запрос", toUser.fullName()),
            String.format("%s rejected your request", toUser.fullName())),
        NotificationType.PERMISSION_REJECTED,
        tokens,
        fromUser,
        null);
  }

  // ======================== PAYMENT PAID ========================

  // buyer to'lov qildi -> seller'ga
  public static FirebaseNotificationReply paymentPaid(
      UUID documentId, UUID toUserId, String docNumber) {
    String n = num(docNumber);
    return withoutToken(
        documentId,
        new TextModel(
            "Shartnoma №" + n + " bo'yicha to'lov amalga oshirildi",
            "Платеж по договору №" + n + " выполнен",
            "Payment for contract №" + n + " has been made"),
        new TextModel(
            "Shartnoma №" + n + " bo'yicha to'lov amalga oshirildi, qabul qilasizmi?",
            "Платеж по договору №" + n + " выполнен, примете ли вы его?",
            "Payment for contract №" + n + " has been made, do you accept?"),
        NotificationType.PAYMENT_PAID,
        toUserId);
  }

  // seller to'lovni tasdiqladi -> buyer'ga
  public static FirebaseNotificationReply paymentPaidApproved(
      UUID documentId, UUID toUserId, String docNumber) {
    String n = num(docNumber);
    return withoutToken(
        documentId,
        new TextModel(
            "Shartnoma №" + n + " to'lovingiz tasdiqlandi",
            "Платеж по договору №" + n + " подтвержден",
            "Payment for contract №" + n + " has been confirmed"),
        new TextModel(
            "Shartnoma №" + n + " to'lovingiz qabul qilindi",
            "Ваш платеж по договору №" + n + " принят",
            "Your payment for contract №" + n + " has been accepted"),
        NotificationType.PAYMENT_PAID_APPROVED,
        toUserId);
  }

  // seller to'lovni rad etdi -> buyer'ga
  public static FirebaseNotificationReply paymentPaidRejected(
      UUID documentId, UUID toUserId, String docNumber) {
    String n = num(docNumber);
    return withoutToken(
        documentId,
        new TextModel(
            "Shartnoma №" + n + " to'lovingiz rad etildi",
            "Платеж по договору №" + n + " отклонен",
            "Payment for contract №" + n + " has been rejected"),
        new TextModel(
            "Shartnoma №" + n + " to'lovingiz rad qilindi",
            "Ваш платеж по договору №" + n + " отклонён",
            "Your payment for contract №" + n + " has been rejected"),
        NotificationType.PAYMENT_PAID_REJECTED,
        toUserId);
  }

  // ======================== PAYMENT DELAY ========================

  // buyer kechiktirish so'radi -> seller'ga
  public static FirebaseNotificationReply paymentDelayRequest(
      UUID documentId, UUID toUserId, String docNumber) {
    String n = num(docNumber);
    return withoutToken(
        documentId,
        new TextModel(
            "Shartnoma №" + n + " to'lov sanasini o'zgartirish so'rovi",
            "Запрос на изменение даты платежа по договору №" + n,
            "Payment date change request for contract №" + n),
        new TextModel(
            "Shartnoma №" + n + " to'lov sanasining o'zgarishini tasdiqlang",
            "Подтвердите изменение даты платежа по договору №" + n,
            "Confirm the payment date change for contract №" + n),
        NotificationType.PAYMENT_DELAY_REQUEST,
        toUserId);
  }

  // seller kechiktirishni tasdiqladi -> buyer'ga
  public static FirebaseNotificationReply paymentDelayApproved(
      UUID documentId, UUID toUserId, String docNumber) {
    String n = num(docNumber);
    return withoutToken(
        documentId,
        new TextModel(
            "Shartnoma №" + n + " to'lov vaqti uzaytirildi",
            "Срок оплаты по договору №" + n + " продлен",
            "Payment date for contract №" + n + " has been extended"),
        new TextModel(
            "Shartnoma №" + n + " to'lov sanasi muvaffaqiyatli o'zgartirildi",
            "Дата платежа по договору №" + n + " успешно изменена",
            "Payment date for contract №" + n + " has been successfully changed"),
        NotificationType.PAYMENT_DELAY_APPROVED,
        toUserId);
  }

  // seller kechiktirishni rad etdi -> buyer'ga
  public static FirebaseNotificationReply paymentDelayRejected(
      UUID documentId, UUID toUserId, String docNumber) {
    String n = num(docNumber);
    return withoutToken(
        documentId,
        new TextModel(
            "Shartnoma №" + n + " to'lov sanasi o'zgarishi rad etildi",
            "Изменение даты платежа по договору №" + n + " отклонено",
            "Payment date change for contract №" + n + " rejected"),
        new TextModel(
            "Shartnoma №" + n + " to'lov sanasini o'zgartirish so'rovi rad etildi",
            "Ваш запрос на изменение даты платежа по договору №" + n + " отклонён",
            "Your request to change payment date for contract №" + n + " has been rejected"),
        NotificationType.PAYMENT_DELAY_REJECTED,
        toUserId);
  }

  // ======================== PAYMENT REQUEST ========================

  // buyer payment request qo'shdi -> seller'ga
  public static FirebaseNotificationReply paymentRequest(
      UUID documentId, UUID toUserId, String docNumber) {
    String n = num(docNumber);
    return withoutToken(
        documentId,
        new TextModel(
            "Shartnoma №" + n + " to'lovni tasdiqlang",
            "Подтвердите платеж по договору №" + n,
            "Confirm payment for contract №" + n),
        new TextModel(
            "Shartnoma №" + n + " bo'yicha to'lov so'rovi keldi",
            "Поступил запрос на оплату по договору №" + n,
            "A payment request for contract №" + n + " has been received"),
        NotificationType.PAYMENT_REQUEST,
        toUserId);
  }

  // seller payment requestni tasdiqladi -> buyer'ga
  public static FirebaseNotificationReply paymentRequestApproved(
      UUID documentId, UUID toUserId, String docNumber) {
    String n = num(docNumber);
    return withoutToken(
        documentId,
        new TextModel(
            "Shartnoma №" + n + " to'lov so'rovingiz tasdiqlandi",
            "Запрос на оплату по договору №" + n + " подтвержден",
            "Payment request for contract №" + n + " has been confirmed"),
        new TextModel(
            "Shartnoma №" + n + " to'lov so'rovingiz qabul qilindi",
            "Ваш запрос на оплату по договору №" + n + " принят",
            "Your payment request for contract №" + n + " has been accepted"),
        NotificationType.PAYMENT_ACCEPT,
        toUserId);
  }

  // seller payment requestni rad etdi -> buyer'ga
  public static FirebaseNotificationReply paymentRequestRejected(
      UUID documentId, UUID toUserId, String docNumber) {
    String n = num(docNumber);
    return withoutToken(
        documentId,
        new TextModel(
            "Shartnoma №" + n + " to'lov so'rovingiz rad etildi",
            "Запрос на оплату по договору №" + n + " отклонен",
            "Payment request for contract №" + n + " has been rejected"),
        new TextModel(
            "Shartnoma №" + n + " to'lov so'rovingiz rad qilindi",
            "Ваш запрос на оплату по договору №" + n + " отклонён",
            "Your payment request for contract №" + n + " has been rejected"),
        NotificationType.PAYMENT_REJECT,
        toUserId);
  }

  // ======================== DOCUMENT ACTIONS ========================

  // guvoh shartnomani qabul qildi -> creator'ga
  public static FirebaseNotificationReply witnessAccepted(
      UUID documentId, UUID toUserId, String docNumber, String witnessFullName) {
    String n = num(docNumber);
    String name = witnessFullName != null ? witnessFullName : "";
    TextModel title1 =
        new TextModel(
            name + " shartnoma №" + n + " ga guvohlikka rozi bo'ldi",
            name + " согласился быть свидетелем договора №" + n,
            name + " agreed to be a witness of contract №" + n);
    return withoutToken(documentId, title1, title1, NotificationType.WITNESS_ACCEPTED, toUserId);
  }

  // guvoh shartnomani rad etdi -> creator'ga
  public static FirebaseNotificationReply witnessRejected(
      UUID documentId, UUID toUserId, String docNumber, String witnessFullName) {
    String n = num(docNumber);
    String name = witnessFullName != null ? witnessFullName : "";
    TextModel title1 =
        new TextModel(
            name + " shartnoma №" + n + " ga guvohlikni rad etdi",
            name + " отказался быть свидетелем договора №" + n,
            name + " rejected to be a witness of contract №" + n);
    return withoutToken(documentId, title1, title1, NotificationType.WITNESS_REJECTED, toUserId);
  }

  // tomon shartnomani imzoladi -> boshqa tomonlarga
  public static FirebaseNotificationReply documentSigned(
      UUID documentId, UUID toUserId, String docNumber, String signerFullName) {
    String n = num(docNumber);
    String name = signerFullName != null ? signerFullName : "";
    TextModel title1 =
        new TextModel(
            name + " shartnoma №" + n + " ni imzoladi",
            name + " подписал договор №" + n,
            name + " signed contract №" + n);
    return withoutToken(documentId, title1, title1, NotificationType.DOCUMENT_SIGNED, toUserId);
  }

  // tomon shartnomani rad etdi -> boshqa tomonlarga
  public static FirebaseNotificationReply documentRejected(
      UUID documentId, UUID toUserId, String docNumber, String rejecterFullName) {
    String n = num(docNumber);
    String name = rejecterFullName != null ? rejecterFullName : "";
    TextModel body1 =
        new TextModel(
            name + " shartnoma №" + n + " ni rad etdi",
            name + " отклонил договор №" + n,
            name + " rejected contract №" + n);
    return withoutToken(documentId, body1, body1, NotificationType.DOCUMENT_REJECTED, toUserId);
  }

  // yaratuvchi shartnomani bekor qildi -> boshqa tomonlarga
  public static FirebaseNotificationReply documentCancelled(
      UUID documentId, UUID toUserId, String docNumber, String cancellerFullName) {
    String n = num(docNumber);
    String name = cancellerFullName != null ? cancellerFullName : "";
    TextModel title1 =
        new TextModel(
            name + " shartnoma №" + n + " ni bekor qildi",
            name + " отменил договор №" + n,
            name + " cancelled contract №" + n);
    return withoutToken(documentId, title1, title1, NotificationType.DOCUMENT_CANCELLED, toUserId);
  }

  // ikkala tomon imzoladi, shartnoma yaratildi -> ikkalasiga + guvohga
  public static FirebaseNotificationReply documentCompleted(
      UUID documentId, UUID toUserId, String docNumber) {
    String n = num(docNumber);
    TextModel title1 =
        new TextModel(
            "Shartnoma №" + n + " muvaffaqiyatli tuzildi",
            "Договор №" + n + " успешно заключен",
            "Contract №" + n + " has been successfully created");
    return withoutToken(documentId, title1, title1, NotificationType.DOCUMENT_COMPLETED, toUserId);
  }

  // hujjat yaratildi -> boshqa tomonga notification
  public static FirebaseNotificationReply documentCreated(
      UUID documentId, UUID toUserId, String docNumber, String creatorFullName) {
    String n = num(docNumber);
    String name = creatorFullName != null ? creatorFullName : "";
    return withoutToken(
        documentId,
        new TextModel(
            name + " sizga shartnoma №" + n + " yubordi",
            name + " отправил вам договор №" + n,
            name + " sent you contract №" + n),
        new TextModel(
            name + " sizga shartnoma №" + n + " yubordi, ko'rib chiqing",
            name + " отправил вам договор №" + n + ", ознакомьтесь",
            name + " sent you contract №" + n + ", please review"),
        NotificationType.DOCUMENT_CREATED,
        toUserId);
  }

  // hujjat yaratildi, shablonda sozlangan custom body bilan -> boshqa tomonga.
  // customBody null/bo'sh → default body. {raqam} → raqam, {yuboruvchi} → yaratuvchi ismi.
  public static FirebaseNotificationReply documentCreated(
      UUID documentId,
      UUID toUserId,
      String docNumber,
      String creatorFullName,
      String customBody) {
    if (customBody == null || customBody.isBlank()) {
      return documentCreated(documentId, toUserId, docNumber, creatorFullName);
    }
    String n = num(docNumber);
    String name = creatorFullName != null ? creatorFullName : "";
    String body = customBody.replace("{raqam}", n).replace("{yuboruvchi}", name);
    return withoutToken(
        documentId,
        new TextModel(
            name + " sizga shartnoma №" + n + " yubordi",
            name + " отправил вам договор №" + n,
            name + " sent you contract №" + n),
        new TextModel(body, body, body),
        NotificationType.DOCUMENT_CREATED,
        toUserId);
  }

  // guvohlikka taklif qilindi -> guvohga
  public static FirebaseNotificationReply witnessInvited(
      UUID documentId, UUID toUserId, String docNumber) {
    String n = num(docNumber);
    return withoutToken(
        documentId,
        new TextModel(
            "Sizni shartnoma №" + n + " ga guvohlikka chaqirishmoqda",
            "Вас приглашают быть свидетелем договора №" + n,
            "You are invited to witness contract №" + n),
        new TextModel(
            "Shartnoma №" + n + " ga guvohlikka chaqirilmoqdasiz",
            "Вас приглашают стать свидетелем договора №" + n,
            "You are being invited to witness contract №" + n),
        NotificationType.WITNESS_INVITED,
        toUserId);
  }

  // ======================== NOTICE (TALABNOMA) ========================

  // talabnoma yaratildi -> debtorga notification
  public static FirebaseNotificationReply noticeCreated(
      UUID documentId, UUID toUserId, String docNumber) {
    String n = num(docNumber);
    TextModel text =
        new TextModel(
            "Hamkoringiz shartnoma №" + n + " bo'yicha talabnoma jo'natdi",
            "Ваш партнер отправил вам претензионное письмо по договору №" + n,
            "Your partner sent you a notice letter for contract №" + n);
    return withoutToken(documentId, text, text, NotificationType.NOTICE_CREATED, toUserId);
  }

  // ======================== REPORT (DA'VO ARIZASI) ========================

  // seller da'vo arizasi yaratdi -> buyer'ga notification
  public static FirebaseNotificationReply reportCreated(
      UUID documentId, UUID toUserId, String docNumber) {
    String n = num(docNumber);
    TextModel text =
        new TextModel(
            "Hamkoringiz shartnoma №" + n + " bo'yicha da'vo arizasini jo'natdi",
            "Ваш партнер отправил исковое заявление по договору №" + n,
            "Your partner has sent a claim statement for contract №" + n);
    return withoutToken(documentId, text, text, NotificationType.REPORT_CREATED, toUserId);
  }

  // ============== Per-template custom body overloads (customBody → push body) ==============
  // customBody null/bo'sh bo'lsa default factory'ga delegate qiladi. {raqam} → shartnoma raqami,
  // {yuboruvchi} → yuboruvchi ismi (mavjud bo'lsa).

  public static FirebaseNotificationReply paymentRequest(
      UUID documentId, UUID toUserId, String docNumber, String customBody) {
    if (customBody == null || customBody.isBlank()) {
      return paymentRequest(documentId, toUserId, docNumber);
    }
    String n = num(docNumber);
    String body = customBody.replace("{raqam}", n);
    TextModel title =
        new TextModel(
            "Shartnoma №" + n + " to'lovni tasdiqlang",
            "Подтвердите платеж по договору №" + n,
            "Confirm payment for contract №" + n);
    return withoutToken(
        documentId, title, new TextModel(body, body, body),
        NotificationType.PAYMENT_REQUEST, toUserId);
  }

  public static FirebaseNotificationReply paymentDelayRequest(
      UUID documentId, UUID toUserId, String docNumber, String customBody) {
    if (customBody == null || customBody.isBlank()) {
      return paymentDelayRequest(documentId, toUserId, docNumber);
    }
    String n = num(docNumber);
    String body = customBody.replace("{raqam}", n);
    TextModel title =
        new TextModel(
            "Shartnoma №" + n + " to'lov sanasini o'zgartirish so'rovi",
            "Запрос на изменение даты платежа по договору №" + n,
            "Payment date change request for contract №" + n);
    return withoutToken(
        documentId, title, new TextModel(body, body, body),
        NotificationType.PAYMENT_DELAY_REQUEST, toUserId);
  }

  public static FirebaseNotificationReply noticeCreated(
      UUID documentId, UUID toUserId, String docNumber, String customBody) {
    if (customBody == null || customBody.isBlank()) {
      return noticeCreated(documentId, toUserId, docNumber);
    }
    String n = num(docNumber);
    String body = customBody.replace("{raqam}", n);
    return withoutToken(
        documentId, new TextModel(body, body, body), new TextModel(body, body, body),
        NotificationType.NOTICE_CREATED, toUserId);
  }

  public static FirebaseNotificationReply reportCreated(
      UUID documentId, UUID toUserId, String docNumber, String customBody) {
    if (customBody == null || customBody.isBlank()) {
      return reportCreated(documentId, toUserId, docNumber);
    }
    String n = num(docNumber);
    String body = customBody.replace("{raqam}", n);
    return withoutToken(
        documentId, new TextModel(body, body, body), new TextModel(body, body, body),
        NotificationType.REPORT_CREATED, toUserId);
  }

  public static FirebaseNotificationReply witnessInvited(
      UUID documentId, UUID toUserId, String docNumber, String customBody) {
    if (customBody == null || customBody.isBlank()) {
      return witnessInvited(documentId, toUserId, docNumber);
    }
    String n = num(docNumber);
    String body = customBody.replace("{raqam}", n);
    TextModel title =
        new TextModel(
            "Sizni shartnoma №" + n + " ga guvohlikka chaqirishmoqda",
            "Вас приглашают быть свидетелем договора №" + n,
            "You are invited to witness contract №" + n);
    return withoutToken(
        documentId, title, new TextModel(body, body, body),
        NotificationType.WITNESS_INVITED, toUserId);
  }

  public static FirebaseNotificationReply documentCancelled(
      UUID documentId,
      UUID toUserId,
      String docNumber,
      String cancellerFullName,
      String customBody) {
    if (customBody == null || customBody.isBlank()) {
      return documentCancelled(documentId, toUserId, docNumber, cancellerFullName);
    }
    String n = num(docNumber);
    String name = cancellerFullName != null ? cancellerFullName : "";
    String body = customBody.replace("{raqam}", n).replace("{yuboruvchi}", name);
    return withoutToken(
        documentId, new TextModel(body, body, body), new TextModel(body, body, body),
        NotificationType.DOCUMENT_CANCELLED, toUserId);
  }

  private static String num(String docNumber) {
    return docNumber != null ? docNumber : "—";
  }
}
