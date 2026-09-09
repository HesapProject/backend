package uz.hesap.service.common.util.message;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {
  // Umumiy
  NEWS(1),
  CONTRACT(2),
  WITNESS(3),

  // Permission (Ruxsatnomalar)
  PERMISSION_REQUEST(10),
  PERMISSION_ACCEPTED(11),
  PERMISSION_CANCELLED(12),
  PERMISSION_REJECTED(13),

  // Passport holatlari
  PASSPORT_ACCEPTED(21),
  PASSPORT_CONFLICTED(22),
  PASSPORT_EXPIRED(23),
  PASSPORT_REJECTED(24),

  // To'lovlar (Payment)
  PAYMENT_REQUEST(30),
  PAYMENT_ACCEPT(31),
  PAYMENT_REJECT(32),

  // To'lov amaliyotlari (Paid Schedule)
  PAYMENT_PAID(33),
  PAYMENT_PAID_APPROVED(34),
  PAYMENT_PAID_REJECTED(35),

  // To'lov kechiktirish (Delay Payment)
  PAYMENT_DELAY_REQUEST(40),
  PAYMENT_DELAY_APPROVED(41),
  PAYMENT_DELAY_REJECTED(42),

  // Hujjat amallari (Document Actions)
  WITNESS_ACCEPTED(50),
  WITNESS_REJECTED(51),
  WITNESS_INVITED(56),
  DOCUMENT_SIGNED(52),
  DOCUMENT_REJECTED(53),
  DOCUMENT_CANCELLED(54),
  DOCUMENT_COMPLETED(55),
  DOCUMENT_CREATED(57),

  // Talabnoma (Notice/Claim Letter)
  NOTICE_CREATED(61),

  // Da'vo arizasi (Report/Claim)
  REPORT_CREATED(60);

  private final int id;

  // ID bo'yicha enumni topish uchun yordamchi metod
  public static NotificationType fromId(int id) {
    for (NotificationType type : values()) {
      if (type.id == id) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown notification id: " + id);
  }
}
