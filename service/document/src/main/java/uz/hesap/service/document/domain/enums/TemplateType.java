package uz.hesap.service.document.domain.enums;

public enum TemplateType {
  // Shartnoma turlari: C2C (jismoniy-jismoniy), B2C (yuridik-jismoniy), B2B (yuridik-yuridik).
  C2C,
  B2C,
  B2B,
  // Quyidagilar shartnoma turi emas: B2B_SPECIAL — variant; NOTICE/REPORT — child shablonlar.
  B2B_SPECIAL,
  NOTICE,
  REPORT
}
