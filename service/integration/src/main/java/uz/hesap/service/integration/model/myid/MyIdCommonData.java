package uz.hesap.service.integration.model.myid;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MyIdCommonData(
    @JsonProperty("first_name") String firstName,
    @JsonProperty("middle_name") String middleName,
    @JsonProperty("last_name") String lastName,
    @JsonProperty("first_name_en") String firstNameEn,
    @JsonProperty("last_name_en") String lastNameEn,
    String pinfl,
    String gender,
    @JsonProperty("birth_place") String birthPlace,
    @JsonProperty("birth_date") String birthDate,
    String nationality,
    String citizenship,
    @JsonProperty("sdk_hash") String sdkHash,
    @JsonProperty("last_update_pass_data") String lastUpdatePassData,
    @JsonProperty("last_update_address") String lastUpdateAddress) {}
