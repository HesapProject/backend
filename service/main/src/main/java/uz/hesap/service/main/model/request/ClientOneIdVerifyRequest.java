package uz.hesap.service.main.model.request;

import io.jsonwebtoken.lang.Assert;

public record ClientOneIdVerifyRequest(
    String code,
    String redirectUrl,
    // device userInfo
    String uuid,
    String osVersion,
    String os,
    String model,
    String brand,
    String type,
    String device,
    String fcmToken) {
  public ClientOneIdVerifyRequest {
    Assert.isTrue(
        code != null && redirectUrl != null && uuid != null, "code ,redirectUrl,uuid is required");
  }
}
