package uz.hesap.service.integration.model.payme;

import java.util.HashMap;

public record Receive(Long id, String method, HashMap<String, Object> params) {
  public static final String CHECK_PERFORM_TRANSACTION = "CheckPerformTransaction";
  public static final String CREATE_TRANSACTION = "CreateTransaction";
  public static final String PERFORM_TRANSACTION = "PerformTransaction";
  public static final String CANCEL_TRANSACTION = "CancelTransaction";
  public static final String CHECK_TRANSACTION = "CheckTransaction";
  public static final String GET_STATEMENT = "GetStatement";
  public static final String SET_FISCAL_DATA = "SetFiscalData";
}
