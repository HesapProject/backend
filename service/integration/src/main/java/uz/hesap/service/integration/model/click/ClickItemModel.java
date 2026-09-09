package uz.hesap.service.integration.model.click;

public record ClickItemModel(
    String Name,
    String SPIC,
    String PackageCode,
    Long Price,
    Long Amount,
    Long VAT,
    Integer VATPercent,
    ComissionInfo CommissionInfo) {}
