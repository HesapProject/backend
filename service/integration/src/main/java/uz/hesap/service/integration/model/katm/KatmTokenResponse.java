package uz.hesap.service.integration.model.katm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// /auth/login javobi. data.accessToken — Bearer JWT (exp: 1 kun), refreshToken.
@JsonIgnoreProperties(ignoreUnknown = true)
public record KatmTokenResponse(KatmError error, Boolean success, Data data) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Data(String accessToken, String refreshToken) {}
}
