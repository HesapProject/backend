package uz.hesap.service.integration.model.ableid;

import org.springframework.util.Assert;

// POST request — secret bo'sh qoldirilsa eski qiymat saqlanadi.
public record AbleIdSettingRequest(
    String baseUrl, String projectId, String secret, String hookUrl) {
  public AbleIdSettingRequest {
    Assert.hasLength(baseUrl, "baseUrl can't be null or empty");
    Assert.hasLength(projectId, "projectId can't be null or empty");
  }
}
