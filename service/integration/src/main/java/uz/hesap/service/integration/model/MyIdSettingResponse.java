package uz.hesap.service.integration.model;

// Response'da secret'lar butunlay ko'rinmaydi (xavfsizlik). `*SecretSet` —
// admin UI uchun har bir secret o'rnatilgan-yo'qligini ko'rsatadi.
public record MyIdSettingResponse(
    String baseUrl,
    String mobileClientId,
    Boolean mobileSecretSet,
    String webClientId,
    Boolean webSecretSet) {}
