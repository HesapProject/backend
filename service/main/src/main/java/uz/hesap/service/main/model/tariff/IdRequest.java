package uz.hesap.service.main.model.tariff;

import java.util.UUID;
import org.springframework.util.Assert;

public record IdRequest(UUID id) {
  public IdRequest {
    Assert.notNull(id, "id is required");
  }
}
