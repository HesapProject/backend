package uz.hesap.service.main.model.request;

import org.immutables.builder.Builder;
import org.springframework.util.Assert;

public record UserPhoneRequest(String phone) {
  @Builder.Constructor
  public UserPhoneRequest {
    Assert.notNull(phone, "phone can't be null or empty");
  }
}
