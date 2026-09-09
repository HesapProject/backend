package uz.hesap.service.main.model.request;

import org.immutables.builder.Builder;
import org.springframework.util.Assert;

public record UsernameRequest(String username) {
  @Builder.Constructor
  public UsernameRequest {
    Assert.notNull(username, "username can't be null or empty");
  }
}
