package uz.hesap.service.integration.model;

import org.springframework.util.Assert;

// POST request — secretKey bo'sh qoldirilsa eski qiymat saqlanadi.
public record TuranixSettingRequest(String baseUrl, String projectId, String secretKey) {
  public TuranixSettingRequest {
    Assert.hasLength(baseUrl, "baseUrl can't be null or empty");
    Assert.hasLength(projectId, "projectId can't be null or empty");
  }
}
