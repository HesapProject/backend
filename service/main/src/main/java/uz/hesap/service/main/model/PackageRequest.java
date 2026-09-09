package uz.hesap.service.main.model;

import org.springframework.util.Assert;
import uz.hesap.service.main.model.tariff.TariffTemplateConfig;
import uz.hesap.service.common.util.TextModel;
import uz.hesap.service.common.util.enums.TariffType;

public record PackageRequest(
    TextModel name,
    TextModel description,
    Integer stars,
    Double price,
    Integer duration,
    TariffType type,
    TariffTemplateConfig templateConfig,
    Integer scoringHesap,
    Integer scoringKatm,
    Integer scoringPayment) {
  public PackageRequest {
    Assert.isTrue(
        stars != null && price != null && name != null && duration != null,
        "price,name,stars,duration is required");
  }
}
