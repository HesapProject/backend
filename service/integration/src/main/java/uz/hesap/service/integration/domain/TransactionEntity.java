package uz.hesap.service.integration.domain;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import uz.hesap.service.integration.domain.enums.BillingType;
import uz.hesap.service.integration.domain.enums.TransactionType;

// Billing schema'dagi mavjud `transaction` jadvaliga ulanadi (DEPOSIT_PAYME/DEPOSIT_CLICK).
@Getter
@Setter
@ToString
@Table(schema = "\"user\"", name = "transaction")
public class TransactionEntity {
  @Id private UUID id;
  private UUID userId;
  private UUID companyId;
  private UUID balanceId;
  private Integer duration;
  private Double amount;
  private TransactionType type;
  private BillingType billingType;
  private String description;
  @CreatedDate private Instant timestamp;
}
