package uz.hesap.service.integration.promos;

// Promokod ishlatilish turi.
public enum PromosUsageType {
  ONE_TIME, // bir martalik — butun tizimda faqat bir marta ishlatiladi
  ONCE_PER_USER, // ko'p kishiga bir martalik — har foydalanuvchi bir marta
  MULTI_USE, // ko'p kishiga ko'p martalik — cheklovsiz
}
