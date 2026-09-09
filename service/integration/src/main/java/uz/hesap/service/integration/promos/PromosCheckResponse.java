package uz.hesap.service.integration.promos;

// Promokod tekshiruvi natijasi — xato tashlamaydi: valid=false bo'lsa sababi message'da.
// valid=true bo'lsa promo (chegirma turi/miqdori bilan) qaytadi.
public record PromosCheckResponse(boolean valid, String message, PromosResponse promo) {}
