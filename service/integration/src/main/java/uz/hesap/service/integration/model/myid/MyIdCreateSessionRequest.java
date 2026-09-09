package uz.hesap.service.integration.model.myid;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.util.Assert;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MyIdCreateSessionRequest(
    @JsonProperty("phone_number") String phoneNumber,
    @JsonProperty("birth_date") String birthDate,
    @JsonProperty("is_resident") Boolean isResident,
    @JsonProperty("pinfl") String pinfl,
    @JsonProperty("pass_data") String passData,
    @JsonProperty("threshold") Float threshold,
    // Client'dan keladi (default MOBILE), MyID'ga YUBORILMAYDI (WRITE_ONLY).
    @JsonProperty(value = "platform", access = JsonProperty.Access.WRITE_ONLY)
        MyIdPlatform platform) {
  public MyIdCreateSessionRequest {
    // phone_number majburiy emas — web SDK oqimi (web.myid.uz) telefon
    // ishlatmaydi, identifikatsiya pinfl + birth_date + yuz orqali.
    Assert.notNull(birthDate, "Birth date is required");
    Assert.notNull(isResident, "Resident status is required");
    Assert.isTrue(passData != null || pinfl != null, "Pinfl or passData required");
    if (threshold == null) {
      threshold = 0.6F;
    }
    if (platform == null) {
      platform = MyIdPlatform.MOBILE;
    }
  }
}
