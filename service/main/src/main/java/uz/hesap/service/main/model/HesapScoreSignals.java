package uz.hesap.service.main.model;

import java.util.List;

// Hesap Score xom signallari (cross-schema document.* dan). Barcha "w*" — yosh-decay
// (half-life) bilan og'irlikli hisoblar; nMatured — muddati kelgan majburiyatlar soni.
public record HesapScoreSignals(
    // S1 — to'lov punktualligi (buyer_in), yosh-decay og'irlikli
    double wEarly,
    double wOnTime,
    double wLate,
    double wVeryLate,
    double wUnpaid,
    long nMatured,
    long unpaidOverdue,
    double maxOverdueDays,
    // S2/S4 — shartnomalar (ikkala taraf)
    long completed,
    long rejected,
    long cancelled,
    long active,
    long counterparties,
    double tenureMonths,
    long totalContracts,
    // S3/S5 — delinquency/friction (userga qarshi)
    long noticesAgainst,
    long claimsAgainst,
    long cancelsApproved,
    long delays,
    // Anti-gaming q uchun — har counterparty bilan shartnoma soni
    List<Long> pairCounts) {

  // Umuman ma'lumot yo'q (butunlay yangi foydalanuvchi).
  public boolean isEmpty() {
    return totalContracts == 0 && nMatured == 0;
  }
}
