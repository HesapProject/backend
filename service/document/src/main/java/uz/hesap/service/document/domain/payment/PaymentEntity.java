package uz.hesap.service.document.domain.payment;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;
import uz.hesap.service.document.domain.enums.Currency;
import uz.hesap.service.document.domain.enums.DocumentStatus;
import uz.hesap.service.document.domain.enums.PaymentScheduleStatus;
import uz.hesap.service.document.util.Constants;

@RequiredArgsConstructor
@Getter
@Setter
@Table(schema = Constants.SCHEMA, name = Constants.TABLE_PAYMENT_SCHEDULE)
public class PaymentEntity {
  @Id private UUID id;
  // Taraf identifikatorlari (PINFL/STIR) — contracts.buyer_in/seller_in'dan backfill.
  private String buyerIn;
  private String sellerIn;
  private UUID contractId;
  private PaymentScheduleStatus status;
  // Shartnoma holati (snapshot) — contracts.status.
  private DocumentStatus contractStatus;
  private Double totalAmount;
  private Double paidAmount = 0.0;
  private Currency currency;
  private UUID currencyId;
  // Shartnomada belgilangan to'lov muddati.
  private Instant contractPaymentDate;
  // Kechiktirilgan/o'zgartirilgan to'lov sanasi (delay tasdiqlanganda).
  private Instant changedPaymentDate;
  // To'liq to'langan vaqt.
  private Instant paidAt;
  private Boolean deleted = Boolean.FALSE;
  @CreatedDate private Instant createdAt;
  @LastModifiedDate private Instant updatedAt;
}
