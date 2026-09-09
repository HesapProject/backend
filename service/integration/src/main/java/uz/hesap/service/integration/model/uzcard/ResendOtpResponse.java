package uz.hesap.service.integration.model.uzcard;

public record ResendOtpResponse(Result result, Object error) {
  public record Result(Integer session, String otpSentPhone) {}
}
