package uz.hesap.service.integration.model.katm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// /auth/init-client javobi. data.pClientId — mijoz identifikatori (KATM-SIR).
@JsonIgnoreProperties(ignoreUnknown = true)
public record KatmInitClientResponse(KatmError error, Boolean success, Data data) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Data(String pClientId) {}
}
