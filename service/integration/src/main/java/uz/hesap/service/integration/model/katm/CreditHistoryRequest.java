package uz.hesap.service.integration.model.katm;

import java.util.UUID;
import org.springframework.util.Assert;

// Tashqi (control/client) so'rov: kredit tarixini so'rashni boshlash. Bu ma'lumotlar
// KATM init-client'ga uzatiladi, so'ng submit-request chaqiriladi.
public record CreditHistoryRequest(
    UUID userId, // ixtiyoriy — qaysi foydalanuvchi nomidan so'ralgani
    String pinfl,
    String docSeries,
    String docNumber,
    String firstName,
    String lastName,
    String middleName,
    String birthDate, // yyyy-mm-dd
    String issueDocDate, // yyyy-mm-dd
    String expiredDocDate, // yyyy-mm-dd
    Integer gender, // 1-erkak / 2-ayol
    String districtId,
    String resAddress,
    String regAddress,
    String phone,
    String language // uz, ru, en
    ) {
  public CreditHistoryRequest {
    Assert.hasLength(pinfl, "pinfl can't be null or empty");
    Assert.hasLength(docSeries, "docSeries can't be null or empty");
    Assert.hasLength(docNumber, "docNumber can't be null or empty");
  }
}
