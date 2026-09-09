package uz.hesap.service.integration.model.eskiz;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EskizAccountRequest(
    @JsonProperty("email") String eskizEmail, @JsonProperty("password") String eskizPassword) {}
