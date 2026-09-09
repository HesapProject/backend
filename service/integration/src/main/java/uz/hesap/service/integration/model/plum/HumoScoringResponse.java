package uz.hesap.service.integration.model.plum;

import java.util.List;
import uz.hesap.service.integration.model.uzcard.ErrorResponse;

public record HumoScoringResponse(Result result, ErrorResponse error) {
  public record Result(
      String cardNumber, String fullName, String connectedPhone, List<Report> report) {}

  public record Report(
      Integer month,
      Integer totalDebitScore, // Балл общего расхода
      Integer totalDebitCount,
      Integer replenishmentScore, // Балл пополнения со счета
      Integer replenishmentCount,
      Integer creditScore, // Балл поступлений на карту (переводы,пополнения через АТМ)
      Integer creditCount) {}
}
