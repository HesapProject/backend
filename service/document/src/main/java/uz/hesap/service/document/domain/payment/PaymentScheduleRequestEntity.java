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
@Table(schema = Constants.SCHEMA, name = Constants.TABLE_PAYMENT_SCHEDULE_REQUEST)
public class PaymentScheduleRequestEntity {
  @Id private UUID id;
  // Taraf identifikatorlari (PINFL/STIR) — contracts'dan.
  private String buyerIn;
  private String sellerIn;
  private String creatorIn; // so'rovni yaratgan taraf (PINFL/STIR)
  private UUID contractId;
  private PaymentScheduleStatus status;
  private UUID paymentId;
  private Double amount;
  private Currency currency;
  private UUID currencyId;
  private Instant paymentDate;
  private String note; // izoh
  private String image; // chek rasmi (CDN URL yoki base64)
  private Boolean deleted = Boolean.FALSE;
  @CreatedDate private Instant createdDate;
  @LastModifiedDate private Instant lastModifiedDate;
}
