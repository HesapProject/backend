package uz.hesap.service.integration.domain;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import uz.hesap.service.common.util.enums.PaymentMethod;

// "user" schema'dagi payments jadvaliga yozadi (balance kabi cross-schema).
// integration'da auditing yo'q — createdAt/updatedAt qo'lda set qilinadi.
@Getter
@Setter
@Table(schema = "\"user\"", name = "payments")
public class PaymentEntity {
  @Id private UUID id;
  private Double amount;
  private UUID userId;
  private PaymentMethod paymentMethod;
  private UUID createdBy;
  private UUID updatedBy;
  private Instant createdAt;
  private Instant updatedAt;
}
