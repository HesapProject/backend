package uz.hesap.service.integration.model;

/** Response DTO for SMS sending result */
public record SmsResponse(String id, String status, String message) {
  public static SmsResponse success(String id) {
    return new SmsResponse(id, "success", "SMS sent successfully");
  }

  public static SmsResponse error(String message) {
    return new SmsResponse(null, "error", message);
  }
}
