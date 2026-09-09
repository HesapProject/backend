package uz.hesap.service.integration.model.uzcard;

public record CreateUserCardResponse(Result result, ErrorResponse error) {
  public record Result(Integer session, String otpSentPhone) {}
}
