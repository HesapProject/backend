package uz.hesap.service.integration.model.payme;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CheckTransactionResult(
    @JsonProperty("create_time") Long createTime,
    @JsonProperty("perform_time") Long performTime,
    @JsonProperty("cancel_time") Long cancelTime,
    String transaction,
    Integer state,
    Integer reason) {}
