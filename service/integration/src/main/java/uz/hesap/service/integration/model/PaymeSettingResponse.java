package uz.hesap.service.integration.model;

// Response'da secret yo'q — faqat secretSet flag.
public record PaymeSettingResponse(String merchantId, Boolean secretSet) {}
