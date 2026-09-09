package uz.hesap.service.integration.model;

import org.springframework.util.Assert;

// POST/PUT request — clientSecret bo'sh qoldirilsa eski qiymat saqlanadi.
public record OneIdSettingRequest(String baseUrl, String clientId, String clientSecret) {
  public OneIdSettingRequest {
    Assert.hasLength(baseUrl, "baseUrl can't be null or empty");
    Assert.hasLength(clientId, "clientId can't be null or empty");
  }
}
