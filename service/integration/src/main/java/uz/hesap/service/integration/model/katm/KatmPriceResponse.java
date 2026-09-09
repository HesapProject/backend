package uz.hesap.service.integration.model.katm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

// "Kredit tarixining narxi" metodi javobi. data.amount — hisobot narxi,
// data.isFree — bepul ko'rsatkichi.
@JsonIgnoreProperties(ignoreUnknown = true)
public record KatmPriceResponse(KatmError error, Boolean success, Data data) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Data(String pClientId, BigDecimal amount, Integer checkId, Boolean isFree) {}
}
