package uz.hesap.service.integration.promos;

// Chegirma turi — discountAmount foizmi yoki aniq summami.
public enum PromosDiscountType {
  PERCENT, // foiz (discountAmount = % qiymat)
  FIXED, // aniq summa (discountAmount = so'mdagi miqdor)
}
