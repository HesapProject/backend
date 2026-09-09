package uz.hesap.service.main.model.request;

import org.immutables.builder.Builder;
import org.springframework.util.Assert;

public record UserConfirmRequest(
    String username,
    String code,
    String uuid,
    String osVersion,
    String os,
    String model,
    String brand,
    String type,
    String device,
    String fcmToken) {
  @Builder.Constructor
  public UserConfirmRequest {
    Assert.notNull(username, "username can't be null or empty");
    Assert.notNull(code, "code can't be null or empty");
    Assert.notNull(uuid, "uuid can't be null or empty");
  }
}
