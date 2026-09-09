package uz.hesap.service.integration.model.myid;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MyIdCreateSessionResponse(@JsonProperty("session_id") String sessionId) {}
