package uz.hesap.service.integration.model;

// Service-to-service ichki javob — clientSecret bilan birga.
// Faqat /api/integration/v1/local/oneid/setting endpoint qaytaradi.
public record OneIdSettingInternal(String baseUrl, String clientId, String clientSecret) {}
