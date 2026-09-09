package uz.hesap.service.integration.model.katm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// /report/get-report javobi. data.reportBase64 — XML formatdagi kredit tarixi
// Base64'da. Hisobot hali tayyor bo'lmasa reportBase64 null bo'lishi mumkin.
@JsonIgnoreProperties(ignoreUnknown = true)
public record KatmGetReportResponse(KatmError error, Boolean success, Data data) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Data(String reportBase64, String resultMessage) {}
}
