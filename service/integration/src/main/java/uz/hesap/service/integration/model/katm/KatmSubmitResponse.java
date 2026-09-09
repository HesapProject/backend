package uz.hesap.service.integration.model.katm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// /report/submit-request javobi. data.pClaimId — ariza raqami,
// data.pToken — get-report uchun token.
@JsonIgnoreProperties(ignoreUnknown = true)
public record KatmSubmitResponse(KatmError error, Boolean success, Data data) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Data(String pClaimId, String language, String pToken, String resultMessage) {}
}
