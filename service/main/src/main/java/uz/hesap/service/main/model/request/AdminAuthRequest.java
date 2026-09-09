package uz.hesap.service.main.model.request;

import org.springframework.util.Assert;

public record AdminAuthRequest(String login, String password) {

  public AdminAuthRequest {
    Assert.hasLength(login, "login can't be null or empty");
    Assert.hasLength(password, "password can't be null or empty");
  }
}
