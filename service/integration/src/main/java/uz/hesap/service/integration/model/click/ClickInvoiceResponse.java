package uz.hesap.service.integration.model.click;

public record ClickInvoiceResponse(Integer error_code, String error_note, Long invoice_id) {}
