package uz.hesap.service.integration.model.myid;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MyIdClientAuthRequest(
    @JsonProperty("client_id") String clientId,
    @JsonProperty("client_secret") String clientSecret) {}
