package uz.hesap.service.integration.model;

import org.springframework.util.Assert;

// POST request — secretKey bo'sh qoldirilsa eski qiymat saqlanadi.
public record ClickSettingRequest(Long serviceId, String merchantId, String secretKey) {
  public ClickSettingRequest {
    Assert.notNull(serviceId, "serviceId can't be null");
    Assert.hasLength(merchantId, "merchantId can't be null or empty");
  }
}
