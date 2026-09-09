package uz.hesap.service.integration.model.payme;

public record FiscalData(
    String receipt_id,
    Integer status_code,
    String message,
    String terminal_id,
    String fiscal_sign,
    String qr_code_url,
    String date) {}
