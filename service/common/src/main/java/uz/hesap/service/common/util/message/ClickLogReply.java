package uz.hesap.service.common.util.message;

import java.time.Instant;
import java.util.UUID;

// Click to'lov gateway so'rovi logini log-servisga yuborish uchun xabar.
// PlumLogReply bilan bir xil shaklda — RabbitMQ routingKey = "ClickLogReply".
// type = oqim nomi (PREPARE, COMPLETE, CANCEL, CHECKOUT_LINK);
// userId/companyId — qaysi biri ma'lum bo'lsa.
public record ClickLogReply(
    UUID userId,
    UUID companyId,
    String type,
    String status,
    String errorMessage,
    String request,
    String response,
    Instant time) {}
