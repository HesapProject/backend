package uz.hesap.service.main.model.request;

import org.immutables.builder.Builder;
import org.springframework.util.Assert;

// SMS code/OneID verify uchun (parol login olib tashlandi).
public record LoginRequest(
    String phone,
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
  public LoginRequest {
    Assert.hasLength(phone, "phone can't be null or empty");
    Assert.notNull(uuid, "UUID can't be null");
  }
}
