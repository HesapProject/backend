package uz.hesap.service.integration.model;

import org.springframework.util.Assert;

// POST/PUT request — har bir secret bo'sh qoldirilsa eski qiymat saqlanadi.
// Mobil va web SDK uchun alohida client juftliklari.
public record MyIdSettingRequest(
    String baseUrl,
    String mobileClientId,
    String mobileClientSecret,
    String webClientId,
    String webClientSecret) {
  public MyIdSettingRequest {
    Assert.hasLength(baseUrl, "baseUrl can't be null or empty");
    Assert.hasLength(mobileClientId, "mobileClientId can't be null or empty");
  }
}
