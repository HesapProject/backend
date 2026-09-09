package uz.hesap.service.main.model.tariff;

import java.util.UUID;
import org.springframework.util.Assert;

// PER_TEMPLATE da count kerak, SHARED da ixtiyoriy
public record TariffTemplateRequest(UUID templateId, Integer count) {
  public TariffTemplateRequest {
    Assert.notNull(templateId, "templateId is required");
  }
}
