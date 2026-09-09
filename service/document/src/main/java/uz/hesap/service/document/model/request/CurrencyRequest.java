package uz.hesap.service.document.model.request;

public record CurrencyRequest(
    String code,
    String nameUz,
    String nameRu,
    String nameEn,
    String symbol,
    Integer sortOrder,
    Boolean isActive) {}
