package uz.hesap.service.integration.model;

// Javobda parol/secret yo'q — faqat *Set flaglar (o'rnatilganmi).
public record RoumingSettingResponse(
    String baseUrl,
    String tokenUrl,
    String login,
    Boolean passwordSet,
    String clientId,
    Boolean clientSecretSet) {}
