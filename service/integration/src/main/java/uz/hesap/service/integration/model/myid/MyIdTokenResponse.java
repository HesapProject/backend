package uz.hesap.service.integration.model.myid;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MyIdTokenResponse(
    @JsonProperty("access_token") String accessToken,
    @JsonProperty("token_type") String tokenType,
    @JsonProperty("expires_in") Integer expiresIn) {}
