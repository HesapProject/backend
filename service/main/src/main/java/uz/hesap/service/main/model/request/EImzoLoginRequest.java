package uz.hesap.service.main.model.request;

import org.immutables.builder.Builder;
import org.springframework.util.Assert;

public record EImzoLoginRequest(
    String uuid,
    String osVersion,
    String os,
    String model,
    String brand,
    String type,
    String device,
    String fcmToken,
    String pkcs7) {

  @Builder.Constructor
  public EImzoLoginRequest {
    Assert.notNull(uuid, "UUID can't be null");
  }
}
