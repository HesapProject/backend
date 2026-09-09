package uz.hesap.service.document.domain.enums;

// Hujjatning maqsadi/turi — oddiy shartnoma yoki maxsus hujjat (TTN/AKT/schyot-faktura).
// Rouming (ЭСФ) integratsiyasi va PDF shabloni shu bo'yicha farqlanishi mumkin.
public enum DocumentPurpose {
  CONTRACT, // Oddiy shartnoma (default)
  TTN, // Tovar-transport nakladnoy
  AKT, // Bajarilgan ish/xizmat dalolatnomasi (Акт выполненных работ)
  FACTURA // Schyot-faktura (ЭСФ)
}
