package uz.hesap.service.document.domain.enums;

// Shartnomani bekor qilish so'rovi holati.
public enum CancelRequestStatus {
  PENDING, // yaratildi, qarshi taraf javobini kutmoqda
  APPROVED, // qarshi taraf tasdiqladi → shartnoma CANCELLED
  REJECTED, // qarshi taraf rad etdi
  CANCELLED, // so'rovchi o'z so'rovini bekor qildi
}
