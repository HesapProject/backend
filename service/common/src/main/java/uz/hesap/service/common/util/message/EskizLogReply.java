package uz.hesap.service.common.util.message;

import java.time.Instant;

// request/response — Eskiz SMS so'rovi va javobi (JSON). Eski kodda yo'q edi,
// admin panelda ko'rsatish uchun qo'shildi (nullable bo'lishi mumkin).
public record EskizLogReply(
    String phone,
    String content,
    Boolean isFailed,
    String error,
    Instant timestamp,
    String request,
    String response) {}
