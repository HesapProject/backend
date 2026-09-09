package uz.hesap.service.integration.model.payme;

import java.util.UUID;

public record PaymeTransactionResponse(
    UUID id,
    String paycomId,
    Long paycomTime,
    UUID orderId,
    Long createTime,
    Long performTime,
    Long cancelTime,
    Integer reason,
    Integer state,
    Integer amount) {}
