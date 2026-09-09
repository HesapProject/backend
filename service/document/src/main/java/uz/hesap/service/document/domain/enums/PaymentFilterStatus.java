package uz.hesap.service.document.domain.enums;

// /payments filtri uchun hisoblangan holatlar (entity status + paid_amount + payment_date'dan):
//  ACTIVE  — muddati o'tgan (PENDING & payment_date < now)
//  PENDING — to'lanmagan, muddati kelmagan (PENDING & paid=0 & payment_date >= now)
//  PARTLY  — qisman to'langan (PENDING & 0 < paid < amount)
//  DONE    — to'liq to'langan (PAID)
public enum PaymentFilterStatus {
  ACTIVE,
  PENDING,
  DONE,
  PARTLY
}
