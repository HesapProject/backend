package uz.hesap.service.integration.model.plum;

public record CreateScoringCardResponse(Result result, Object error) {
  public record Result(Integer scoringId) {}
}
