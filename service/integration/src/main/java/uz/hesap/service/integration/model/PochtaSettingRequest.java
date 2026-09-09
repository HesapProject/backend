package uz.hesap.service.integration.model;

import org.springframework.util.Assert;

// POST — password bo'sh qoldirilsa eski qiymat saqlanadi.
public record PochtaSettingRequest(String baseUrl, String username, String password) {
  public PochtaSettingRequest {
    Assert.hasLength(baseUrl, "baseUrl can't be null or empty");
    Assert.hasLength(username, "username can't be null or empty");
  }
}
