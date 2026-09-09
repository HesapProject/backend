package uz.hesap.service.document.domain.enums;

// Mahsulot so'rovi holati.
public enum ProductRequestStatus {
  PENDING, // yaratildi, qarshi taraf javobini kutmoqda
  APPROVED, // qarshi taraf tasdiqladi
  REJECTED, // qarshi taraf rad etdi
  CANCELLED, // so'rovchi o'z so'rovini bekor qildi
}
