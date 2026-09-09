package uz.hesap.service.integration.model;

// Response'da secretKey yo'q — faqat keySet flag.
public record ClickSettingResponse(Long serviceId, String merchantId, Boolean keySet) {}
