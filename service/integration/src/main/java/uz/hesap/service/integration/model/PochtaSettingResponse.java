package uz.hesap.service.integration.model;

// Response'da password yo'q — faqat passwordSet flag.
public record PochtaSettingResponse(String baseUrl, String username, Boolean passwordSet) {}
