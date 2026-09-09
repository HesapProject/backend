package uz.hesap.service.common.util;

import java.io.Serializable;
import java.util.UUID;

// apiKey — OpenAPI kaliti bilan kelgan so'rovda to'ldiriladi (JWT oqimida null).
// Eski 3-argumentli chaqiriqlar buzilmasligi uchun qisqa konstruktor saqlanadi.
public record UserPrincipal(
    UserResponse user, String token, UUID sessionId, ApiKeyContext apiKey)
    implements Serializable {

  public UserPrincipal(final UserResponse user, final String token, final UUID sessionId) {
    this(user, token, sessionId, null);
  }
}
