package uz.hesap.service.main.domain;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;
import uz.hesap.service.common.util.enums.PaymentMethod;
import uz.hesap.service.main.util.Constants;

// To'lovlar jurnali — Payme/Click/Control'dan kelgan har to'lov shu yerga yoziladi.
@Getter
@Setter
@Table(schema = Constants.SCHEMA, name = Constants.TABLE_PAYMENTS)
public class PaymentEntity {
  @Id private UUID id;
  private Double amount;
  private UUID userId;
  private PaymentMethod paymentMethod;
  // Auditor bean yo'q — createdBy/updatedBy qo'lda set qilinadi (webhook'da null).
  private UUID createdBy;
  private UUID updatedBy;
  @CreatedDate private Instant createdAt;
  @LastModifiedDate private Instant updatedAt;
}
