package uz.hesap.service.common.util.message;

import java.time.Instant;
import java.util.UUID;

// Turanix partnyor-servis so'rovi logini log-servisga yuborish uchun xabar.
// PlumLogReply bilan bir xil shaklda — RabbitMQ routingKey = "TuranixLogReply".
public record TuranixLogReply(
    UUID userId,
    String msisdn,
    String pinfl,
    String status,
    String errorMessage,
    String request,
    String response,
    Instant time) {}
