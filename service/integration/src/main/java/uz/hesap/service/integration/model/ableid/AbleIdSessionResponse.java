package uz.hesap.service.integration.model.ableid;

// Mobil SDK uchun: attemptId + baseUrl (SDK domenni kutadi), fullUrl — web fallback.
public record AbleIdSessionResponse(String attemptId, String baseUrl, String fullUrl) {}
