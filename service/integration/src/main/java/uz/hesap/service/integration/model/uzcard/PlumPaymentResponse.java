package uz.hesap.service.integration.model.uzcard;

// Kartadan to'lov natijasi (ichki API javobi).
// otpRequired=true bo'lsa — karta tasdiqlashni talab qiladi: session bilan /payment/confirm chaqiriladi.
// otpRequired=false — to'lov bajarildi, balance yangilandi.
public record PlumPaymentResponse(
    boolean otpRequired,
    Long session,
    String otpSentPhone,
    Long transactionId,
    String utrno,
    String cardNumber,
    Double amount,
    Double balance) {

  public static PlumPaymentResponse otpRequired(
      Long session, String otpSentPhone, Long transactionId, Double amount) {
    return new PlumPaymentResponse(
        true, session, otpSentPhone, transactionId, null, null, amount, null);
  }

  public static PlumPaymentResponse completed(
      Long transactionId, String utrno, String cardNumber, Double amount, Double balance) {
    return new PlumPaymentResponse(
        false, null, null, transactionId, utrno, cardNumber, amount, balance);
  }
}
