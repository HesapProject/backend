package uz.hesap.service.common.util.message;

import java.time.Instant;
import java.util.UUID;

// Rouming (Factura Provider — ЭСФ) draft yuborish logini log-servisga yuborish
// uchun xabar. TuranixLogReply bilan bir xil shaklda — routingKey = "RoumingLogReply".
public record RoumingLogReply(
    UUID contractId,
    String contractNo,
    String facturaNo,
    String sellerTin,
    String buyerTin,
    String status,
    String errorMessage,
    String request,
    String response,
    Instant time) {}
