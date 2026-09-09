package uz.hesap.service.integration.model.uzcard;

// Plum /Payment/payment javobi.
// - Trusted karta: utrno to'ladi (bajarilgan).
// - Trusted bo'lmasa/sendOtp: session + otpSentPhone to'ladi (OTP yuborildi, ConfirmPayment kerak).
public record PaymentResponse(Result result, ErrorResponse error) {
  public record Result(
      Long transactionId,
      String utrno,
      String terminalId,
      String merchantId,
      String cardNumber,
      String date,
      Long session,
      String otpSentPhone) {}
}
