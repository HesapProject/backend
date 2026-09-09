package uz.hesap.service.integration.model.payme;

public record Items(
    String title,
    Long discount,
    Long price,
    Integer count,
    String code,
    String package_code,
    Integer vat_percent) {}
