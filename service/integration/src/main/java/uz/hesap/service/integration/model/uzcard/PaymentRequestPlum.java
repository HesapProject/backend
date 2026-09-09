package uz.hesap.service.integration.model.uzcard;

// Plum /Payment/payment so'rovi (biriktirilgan kartadan to'lov).
// userId — mijoz tarafidagi id (karta yaratishda yuborilgan); cardId — Plum tizimidagi karta id (Long).
// sendOtp=false — bir bosqichli (trusted karta); extraId har tranzaksiyada unikal bo'lishi shart.
public record PaymentRequestPlum(
    String userId,
    Long cardId,
    Double amount,
    String extraId,
    Boolean sendOtp,
    String transactionData) {}
