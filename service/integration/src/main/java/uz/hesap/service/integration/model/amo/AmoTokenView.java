package uz.hesap.service.integration.model.amo;

import java.time.Instant;

// GET /token uchun tokenning maxfiy bo'lmagan ko'rinishi (access/refresh oshkor qilinmaydi).
public record AmoTokenView(
    boolean present, Long expiresIn, Long serverTime, Instant createdDate) {}
