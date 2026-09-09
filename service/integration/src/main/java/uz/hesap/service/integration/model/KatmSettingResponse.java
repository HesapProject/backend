package uz.hesap.service.integration.model;

// Response'da password yo'q — faqat passwordSet flag.
public record KatmSettingResponse(String baseUrl, String login, Boolean passwordSet) {}
