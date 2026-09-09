package uz.hesap.service.integration.model;

import org.springframework.util.Assert;

// POST/PUT request — password bo'sh qoldirilsa eski qiymat saqlanadi.
public record PlumSettingRequest(String baseUrl, String login, String password) {
  public PlumSettingRequest {
    Assert.hasLength(baseUrl, "baseUrl can't be null or empty");
    Assert.hasLength(login, "login can't be null or empty");
  }
}
