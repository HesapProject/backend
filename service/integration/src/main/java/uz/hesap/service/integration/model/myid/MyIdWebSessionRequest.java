package uz.hesap.service.integration.model.myid;

import com.fasterxml.jackson.annotation.JsonProperty;

// MyID Web SDK session yaratish so'rovi: POST /api/v1/web/sessions.
// pinfl/birth_date bu yerda EMAS — ular web.myid.uz URL'iga qo'shiladi.
public record MyIdWebSessionRequest(
    @JsonProperty("max_retries") Integer maxRetries,
    @JsonProperty("external_id") String externalId,
    @JsonProperty("ip_address") String ipAddress) {}
