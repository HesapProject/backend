package uz.hesap.service.integration.model.myid;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MyIdAuthRequest(
    @JsonProperty("grant_type") String grantType,
    @JsonProperty("code") String code,
    @JsonProperty("client_id") String clientId,
    @JsonProperty("client_secret") String clientSecret,
    @JsonProperty("refresh_token") String refreshToken,
    @JsonProperty("username") String username,
    @JsonProperty("password") String password,
    @JsonProperty("scope") String scope,
    @JsonProperty("method") String method,
    @JsonProperty("redirect_uri") String redirectUri) {}
