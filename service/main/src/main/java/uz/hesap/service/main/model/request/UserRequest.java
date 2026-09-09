package uz.hesap.service.main.model.request;

import org.immutables.builder.Builder;
import org.springframework.util.Assert;

public record UserRequest(String firstName, String lastName, String phone, String code) {
  @Builder.Constructor
  public UserRequest {
    Assert.notNull(phone, "phone can't be null or empty");
  }
}
