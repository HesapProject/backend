package uz.hesap.service.integration.model;

// Response'da clientSecret yo'q — faqat secretSet flag.
public record OneIdSettingResponse(String baseUrl, String clientId, Boolean secretSet) {}
