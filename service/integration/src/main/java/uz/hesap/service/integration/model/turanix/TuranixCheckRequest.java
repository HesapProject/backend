package uz.hesap.service.integration.model.turanix;

import java.util.UUID;
import org.springframework.util.Assert;

// Tashqi (control/client) so'rov: MSISDN pasport/PINFL'ga biriktirilganini tekshirish.
// msisdn + pinfl majburiy; passSer/passNum ixtiyoriy. userId — kim nomidan so'ralgani.
public record TuranixCheckRequest(
    UUID userId, String msisdn, String pinfl, String passSer, String passNum) {
  public TuranixCheckRequest {
    Assert.hasLength(msisdn, "msisdn can't be null or empty");
    Assert.hasLength(pinfl, "pinfl can't be null or empty");
  }
}
