package uz.hesap.service.document.model.response;

import java.util.List;
import java.util.Map;
import uz.hesap.service.document.domain.enums.Currency;
import uz.hesap.service.document.domain.enums.Direction;
import uz.hesap.service.document.domain.enums.DocumentStatus;

/** Biznes statistikasi — vendor dashboard uchun birlashtirilgan javob. */
public record BusinessStatsResponse(
    Map<DocumentStatus, Long> contractStatusCounts,
    List<ContractCurrencyStats> activeContracts,
    List<PaymentFlowStats> receivables,
    List<PaymentFlowStats> payables,
    List<CashflowPoint> cashflow) {

  /** Faol shartnomalar — currency kesimida soni va umumiy qiymati. */
  public record ContractCurrencyStats(Currency currency, long count, double totalAmount) {}

  /**
   * To'lov oqimi — currency kesimida. pending* — qolgan (amount - paidAmount), overdue* — muddati
   * o'tgan pending qismi, paid* — to'langan.
   */
  public record PaymentFlowStats(
      Currency currency,
      long pendingCount,
      double pendingAmount,
      long overdueCount,
      double overdueAmount,
      long paidCount,
      double paidAmount) {}

  /** Oylik pul oqimi nuqtasi. paid=true — amalga oshgan, false — kutilayotgan (PENDING). */
  public record CashflowPoint(
      String month, Currency currency, Direction direction, boolean paid, double amount) {}
}
