package uz.hesap.service.integration.model.amo;

// amoCRM OAuth boshlang'ich ulanish (authorization_code). client_id/redirect config'dan,
// clientSecret va code qo'lda beriladi (bir martalik ulanish).
public record AmoInitRequest(String clientSecret, String code) {}
