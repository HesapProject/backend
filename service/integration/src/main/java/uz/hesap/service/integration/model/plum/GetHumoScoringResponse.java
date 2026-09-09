package uz.hesap.service.integration.model.plum;

import java.util.List;

public record GetHumoScoringResponse(Result result, Object error) {

  public record Result(List<Item> item) {}

  public record Item(
      Integer cardCount,
      Integer month,
      Integer totalDebitScore,
      Integer totalDebitCount,
      Integer replenishmentScore,
      Integer replenishmentCount,
      Integer creditScore,
      Integer creditCount) {}
}
