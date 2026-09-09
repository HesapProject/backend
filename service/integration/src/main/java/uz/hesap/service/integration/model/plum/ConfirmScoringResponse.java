package uz.hesap.service.integration.model.plum;

public record ConfirmScoringResponse(Result result, Object error) {

  public record Result(Integer scoringId) {}
}
