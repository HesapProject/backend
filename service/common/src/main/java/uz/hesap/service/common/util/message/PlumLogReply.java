package uz.hesap.service.common.util.message;

import java.time.Instant;
import java.util.UUID;

// Plum (skoring) integratsiyasi so'rovi logini log-servisga yuborish uchun xabar.
// MyIdLogReply bilan bir xil shaklda — RabbitMQ routingKey = "PlumLogReply".
public record PlumLogReply(
    UUID userId,
    UUID cardId,
    String type,
    String status,
    String errorMessage,
    String request,
    String response,
    Instant time) {}
