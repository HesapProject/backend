package uz.hesap.service.integration.model;

import org.springframework.util.Assert;

// POST request — secret bo'sh qoldirilsa eski qiymat saqlanadi.
public record PaymeSettingRequest(String merchantId, String secret) {
  public PaymeSettingRequest {
    Assert.hasLength(merchantId, "merchantId can't be null or empty");
  }
}
