package uz.hesap.service.common.util.message;

import java.time.Instant;
import java.util.UUID;

// Payme to'lov gateway so'rovi logini log-servisga yuborish uchun xabar.
// PlumLogReply bilan bir xil shaklda — RabbitMQ routingKey = "PaymeLogReply".
// type = oqim nomi (CHECK_PERFORM, CREATE_TRANSACTION, PERFORM, CANCEL, CHECK,
// GET_STATEMENT, CHECKOUT_LINK); userId/companyId — qaysi biri ma'lum bo'lsa.
public record PaymeLogReply(
    UUID userId,
    UUID companyId,
    String type,
    String status,
    String errorMessage,
    String request,
    String response,
    Instant time) {}
