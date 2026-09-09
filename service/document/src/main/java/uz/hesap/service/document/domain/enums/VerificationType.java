package uz.hesap.service.document.domain.enums;

/**
 * Shartnoma tarafini tasdiqlash usuli. Shablon darajasida belgilanadi: jismoniy va
 * yuridik shaxslar uchun alohida.
 *
 * <ul>
 *   <li>NONE — tasdiqsiz ("oddiy"): kod/yuz talab qilinmaydi, to'g'ridan-to'g'ri qabul.
 *   <li>OTP_SMS — SMS orqali bir martalik kod (faqat jismoniy shaxs).
 *   <li>MY_ID — yuz tasdiqlash + pasport (face-check).
 *   <li>E_IMZO — elektron raqamli imzo (PKCS#7).
 * </ul>
 */
public enum VerificationType {
  NONE,
  OTP_SMS,
  MY_ID,
  ABLE_ID,
  E_IMZO
}
