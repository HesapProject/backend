package uz.hesap.service.document.util;

public record Constants() {

  public static final String SCHEMA = "document";
  public static final String TABLE_TEMPLATE = "contract_template";
  public static final String TABLE_NOTICE_TEMPLATE = "notice_template";
  public static final String TABLE_CLAIM_TEMPLATE = "claim_template";
  public static final String TABLE_TEMPLATE_FIELD = "template_field";
  public static final String TABLE_TEMPLATE_NOTIFICATION = "template_notification";
  public static final String TABLE_DOCUMENT = "contracts";
  public static final String TABLE_DOCUMENT_VALUE = "contract_value";
  public static final String TABLE_PAYMENT_SCHEDULE = "payments";
  public static final String TABLE_PAID_SCHEDULE = "payment_transactions";
  public static final String TABLE_PAYMENT_SCHEDULE_REQUEST = "payment_requests";
  public static final String TABLE_DELAY_REQUEST = "delay_requests";
  public static final String TABLE_DOCUMENT_SIGNATURE = "document_signature";
  public static final String TABLE_NOTICE = "notices";
  public static final String TABLE_CLAIMS = "claims";
  public static final String TABLE_CURRENCY = "currencies";
  public static final String TABLE_CONTRACT_PRODUCT = "products";
  public static final String TABLE_CONTRACT_WITNESS = "witnesses";
  public static final String TABLE_WITNESS_REQUEST = "witness_requests";
  public static final String TABLE_CANCEL_REQUEST = "cancel_requests";
  public static final String TABLE_PRODUCT_REQUEST = "product_requests";
}
