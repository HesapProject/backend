package uz.hesap.service.integration.model.myid;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public record MyIdReuid(@JsonProperty("expires_at") Long expiresAt, UUID value) {}
