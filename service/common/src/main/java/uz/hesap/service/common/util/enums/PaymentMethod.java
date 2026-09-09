package uz.hesap.service.common.util.enums;

// To'lov manbasi. payments: PAYME/CLICK/PLUM (integration), CONTROL (admin).
// purchases: BALANCE (balansdan), CARD, yoki PAYME/CLICK (to'g'ridan-to'g'ri).
public enum PaymentMethod {
  PAYME,
  CLICK,
  PLUM,
  CONTROL,
  BALANCE,
  CARD
}
