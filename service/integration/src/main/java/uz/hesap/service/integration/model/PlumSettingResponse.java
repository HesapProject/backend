package uz.hesap.service.integration.model;

// Response'da password yo'q — faqat passwordSet flag.
public record PlumSettingResponse(String baseUrl, String login, Boolean passwordSet) {}
