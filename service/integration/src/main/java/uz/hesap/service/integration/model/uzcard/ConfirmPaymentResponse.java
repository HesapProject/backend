package uz.hesap.service.integration.model.uzcard;

// Plum /Payment/confirmPayment javobi. status=1 — muvaffaqiyat (4.1 STATUSY TRANZAKSIY).
// cardId (Plum Long) + amount — balansni to'g'ri to'ldirish uchun ishlatiladi.
public record ConfirmPaymentResponse(Result result, ErrorResponse error) {
  public record Result(
      Long transactionId,
      String utrno,
      Integer status,
      String statusComment,
      String terminalId,
      String merchantId,
      String cardNumber,
      String date,
      Double amount,
      Long cardId,
      Double commission,
      Double totalAmount,
      String transactionData) {}
}
