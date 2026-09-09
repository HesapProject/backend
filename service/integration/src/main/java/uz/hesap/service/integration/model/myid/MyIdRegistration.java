package uz.hesap.service.integration.model.myid;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MyIdRegistration(
    String mfy,
    @JsonProperty("mfy_id") String mfyId,
    String region,
    String address,
    String country,
    String cadastre,
    String district,
    @JsonProperty("region_id") String regionId,
    @JsonProperty("country_id") String countryId,
    @JsonProperty("district_id") String districtId,
    @JsonProperty("region_id_cbu") String regionIdCbu,
    @JsonProperty("country_id_cbu") String countryIdCbu,
    @JsonProperty("district_id_cbu") String districtIdCbu,
    @JsonProperty("registration_date") String registrationDate,
    @JsonProperty("date_from") String dateFrom,
    @JsonProperty("date_till") String dateTill) {}
