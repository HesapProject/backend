package uz.hesap.service.document.domain.enums;

/**
 * Rouming (Factura Provider) hujjat turi — shablonda tanlanadi. Oldi-berdi
 * tasdiqlanganda shu turga mos draft hujjat yuboriladi (hozircha faqat FACTURA
 * amalda; qolganlari tanlab qo'yiladi, provider endpointi qo'shilganda ishlaydi).
 * NULL → Rouming'ga yuborilmaydi.
 */
public enum RoumingType {
  NONE, // Rouming'ga yuborilmaydi (aniq tanlov; null ham shu ma'noda)
  FACTURA, // Hisobvaraq-faktura (ЭСФ)
  ACT, // Bajarilgan ish/xizmat dalolatnomasi
  EMPOWERMENT, // Ishonchnoma
  WAYBILL, // TTN / yuk xati
  CONTRACT, // Shartnoma
  VERIFICATION_ACT // Solishtirish dalolatnomasi
}
