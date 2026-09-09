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
import uz.hesap.service.document.domain.enums.PaymentScheduleStatus;
import uz.hesap.service.document.util.Constants;

@RequiredArgsConstructor
@Getter
@Setter
@Table(schema = Constants.SCHEMA, name = Constants.TABLE_PAID_SCHEDULE)
public class PaidScheduleEntity {
  @Id private UUID id;
  // Taraf identifikatorlari (PINFL/STIR) — contracts'dan, PaymentEntity bilan izchil.
  private String buyerIn;
  private String sellerIn;
  private UUID documentId;
  private UUID paymentScheduleId;
  private PaymentScheduleStatus status;
  private Double amount;
  private Currency currency;
  private UUID currencyId;
  private Instant paymentDate;
  // To'lov cheki rasm URL (xaridor yuklagan, ixtiyoriy).
  private String proof;
  private Boolean deleted = Boolean.FALSE;
  @CreatedDate private Instant createdDate;
  @LastModifiedDate private Instant lastModifiedDate;
}
