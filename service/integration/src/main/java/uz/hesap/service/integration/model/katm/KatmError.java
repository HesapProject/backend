package uz.hesap.service.integration.model.katm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// KATM javoblaridagi umumiy xato bloki: error.errId + error.errMsg.
@JsonIgnoreProperties(ignoreUnknown = true)
public record KatmError(Integer errId, String errMsg) {}
