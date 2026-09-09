package uz.hesap.service.integration.model.turanix;

import com.fasterxml.jackson.annotation.JsonProperty;

// Turanix /api/v1/check-pass-msisdn so'rov tanasi. Maydonlar snake_case'da
// (Turanix API talabi). msisdn + pinfl majburiy, pasport seriya/raqami ixtiyoriy.
public record CheckPassMsisdnRequest(
    @JsonProperty("msisdn") String msisdn,
    @JsonProperty("pinfl") String pinfl,
    @JsonProperty("pass_ser") String passSer,
    @JsonProperty("pass_num") String passNum) {}
