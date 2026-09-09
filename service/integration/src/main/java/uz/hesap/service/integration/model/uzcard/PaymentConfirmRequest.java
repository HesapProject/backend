package uz.hesap.service.integration.model.uzcard;

// To'lovni OTP bilan tasdiqlash (ichki API + Plum /Payment/confirmPayment bir xil shakl).
// session — /payment javobida kelgan; otp — SMS kodi.
public record PaymentConfirmRequest(Long session, String otp) {}
