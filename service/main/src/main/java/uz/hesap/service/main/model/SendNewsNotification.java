package uz.hesap.service.main.model;

import java.util.UUID;
import org.springframework.util.Assert;

public record SendNewsNotification(UUID blogId) {
  public SendNewsNotification {
    Assert.notNull(blogId, "News id required");
  }
}
