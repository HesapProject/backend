package uz.hesap.service.integration.model.payme;

import java.util.UUID;

public record OrderTransaction(
    UUID id,
    String paycomId,
    Long paycomTime,
    UUID orderId,
    Long createTime,
    Long performTime,
    Long cancelTime,
    Integer reason,
    Integer state,
    Integer amount) {

  public static final Integer STATE_NEW = 0;
  public static final Integer STATE_IN_PROGRESS = 1;
  public static final Integer STATE_DONE = 2;
  public static final Integer STATE_CANCELED = -1;
  public static final Integer STATE_POST_CANCELED = -2;

  public static final Integer RECEIVER_NOT_FOUND = 1;
  public static final Integer DEBIT_OPERATION_ERROR = 2;
  public static final Integer TRANSACTION_ERROR = 3;
  public static final Integer TRANSACTION_TIMEOUT = 4;
  public static final Integer MONEY_BACK = 5;
  public static final Integer UNKNOWN_ERROR = 10;
}
