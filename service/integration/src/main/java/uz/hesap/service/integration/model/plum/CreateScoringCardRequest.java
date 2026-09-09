package uz.hesap.service.integration.model.plum;

import java.time.LocalDate;
import java.util.UUID;

public record CreateScoringCardRequest(UUID cardId, LocalDate beginDate, LocalDate endDate, UUID userPackageId) {
  public CreateScoringCardRequest {
    if (endDate == null) {
      endDate = LocalDate.now();
    }
    if (beginDate == null) {
      beginDate = endDate.minusMonths(6);
    }
  }
}
