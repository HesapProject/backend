package uz.hesap.service.integration.model.turanix;

import com.fasterxml.jackson.annotation.JsonProperty;

// Turanix /api/v1/check-pass-msisdn javobi. Masalan:
// {"result":"ok","code":3000,"description":"MSISDN oformlen na etot pasport..."}
public record CheckPassMsisdnResponse(
    @JsonProperty("result") String result,
    @JsonProperty("code") Integer code,
    @JsonProperty("description") String description) {}
