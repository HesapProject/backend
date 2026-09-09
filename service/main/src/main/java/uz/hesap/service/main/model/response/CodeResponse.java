package uz.hesap.service.main.model.response;

import java.time.Instant;

/** Response DTO for device authentication (SMS flow) */
public record CodeResponse(String phone, Instant sendTime, Integer time) {}
