package uz.hesap.service.integration.model.plum;

import java.util.List;
import uz.hesap.service.integration.model.uzcard.ErrorResponse;

public record ScoringGetPointResponse(Result result, ErrorResponse error) {
  public record Result(
      Double maxScoreBall,
      Double scoredBall,
      String jsonBody,
      List<ScoreItem> scoreList,
      Integer scoringId) {}

  public record ScoreItem(
      String templateName, String categoryName, Double ball, String scoringId) {}
}
