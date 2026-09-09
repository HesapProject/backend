package uz.hesap.service.integration.model;

// Response'da secretKey yo'q — faqat secretSet flag.
public record TuranixSettingResponse(String baseUrl, String projectId, Boolean secretSet) {}
