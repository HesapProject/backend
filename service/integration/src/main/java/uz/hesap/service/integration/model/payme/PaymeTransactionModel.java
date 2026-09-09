package uz.hesap.service.integration.model.payme;

import uz.hesap.service.integration.util.PaymeInterface;

public record PaymeTransactionModel(
    String id,
    Long time,
    Integer amount,
    Account account,
    Long create_time,
    Long perform_time,
    Long cancel_time,
    String transaction,
    Integer state,
    Integer reason)
    implements PaymeInterface {}
