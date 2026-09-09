package uz.hesap.service.integration.model;

import org.springframework.util.Assert;

// Factura.uz sozlamasi (POST). clientSecret/password bo'sh qoldirilsa eski qiymat
// saqlanadi (tahrirlashda qayta kiritish shart emas). baseUrl/tokenUrl bo'sh bo'lsa
// yml default ishlatiladi.
public record RoumingSettingRequest(
    String baseUrl,
    String tokenUrl,
    String login,
    String password,
    String clientId,
    String clientSecret) {
  public RoumingSettingRequest {
    Assert.hasLength(login, "login (username) can't be null or empty");
    Assert.hasLength(clientId, "clientId can't be null or empty");
  }
}
