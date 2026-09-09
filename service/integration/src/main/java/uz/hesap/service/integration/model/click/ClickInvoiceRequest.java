package uz.hesap.service.integration.model.click;

public record ClickInvoiceRequest(
    Long service_id, Double amount, String phone_number, String merchant_trans_id) {}
