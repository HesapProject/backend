package uz.hesap.service.integration.model.ableid;

// Response'da secret yo'q — faqat secretSet flag.
public record AbleIdSettingResponse(
    String baseUrl, String projectId, Boolean secretSet, String hookUrl) {}
