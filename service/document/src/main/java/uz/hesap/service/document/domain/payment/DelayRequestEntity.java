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

// Kechiktirish so'rovlari — payment_requests bilan bir xil struktura.
@RequiredArgsConstructor
@Getter
@Setter
@Table(schema = Constants.SCHEMA, name = Constants.TABLE_DELAY_REQUEST)
public class DelayRequestEntity {
  @Id private UUID id;
  private String buyerIn;
  private String sellerIn;
  private UUID contractId;
  private PaymentScheduleStatus status;
  private UUID paymentId;
  private Double amount;
  private Currency currency;
  private UUID currencyId;
  private Instant paymentDate;
  private String note; // kechiktirish sababi
  private Boolean deleted = Boolean.FALSE;
  @CreatedDate private Instant createdDate;
  @LastModifiedDate private Instant lastModifiedDate;
}
