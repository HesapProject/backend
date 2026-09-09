package uz.hesap.service.integration.model.plum;

import java.util.List;

public record GetScoringResponse(Result result, Object error) {

  public record Result(List<Item> items) {}

  public record Item(String category, String templateDetails, Integer ball) {}
}
