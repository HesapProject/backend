package uz.hesap.service.integration.model.myid;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MyIdAddress(
    @JsonProperty("permanent_address") String permanentAddress,
    @JsonProperty("temporary_address") String temporaryAddress,
    @JsonProperty("permanent_registration") MyIdRegistration permanentRegistration,
    @JsonProperty("temporary_registration") MyIdRegistration temporaryRegistration) {}
