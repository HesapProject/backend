package uz.hesap.service.integration.model.plum;

public record CreateScoringResponse(Result result, Object error) {

  public record Result(Integer session, String otpSentPhone) {}
}
