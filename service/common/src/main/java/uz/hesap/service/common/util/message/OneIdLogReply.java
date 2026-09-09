package uz.hesap.service.common.util.message;

import java.time.Instant;
import java.util.UUID;
import uz.hesap.service.common.util.UserResponse;

// OneID log xabari. Avval faqat (UserResponse, companyId, time) edi.
// Endi OneID so'rovi/javobi (request/response) ham yoziladi — admin panelda
// ko'rsatish uchun. Eski chaqiruvlar (CompanyService) uchun pastdagi
// backward-compat konstruktor saqlanadi.
public record OneIdLogReply(
    UUID userId,
    String firstName,
    String lastName,
    UUID companyId,
    String request,
    String response,
    String status,
    String errorMessage,
    Instant time) {

  // CompanyService eski chaqirig'i: (UserResponse, companyId, time).
  public OneIdLogReply(UserResponse user, UUID companyId, Instant time) {
    this(
        user != null ? user.id() : null,
        user != null ? user.firstName() : null,
        user != null ? user.lastName() : null,
        companyId,
        null,
        null,
        "SUCCESS",
        null,
        time);
  }
}
