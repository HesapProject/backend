package uz.hesap.service.integration.domain;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import uz.hesap.service.integration.domain.enums.BillingType;

// Billing schema'dagi mavjud `click` jadvaliga ulanadi.
@Getter
@Setter
@ToString
@Table(schema = "billing", name = "click")
public class ClickEntity {
  @Id private UUID id;
  private UUID uuid;
  private BillingType billingType;
  private String clickTransId;
  private Long serviceId;
  private Long clickPaydocId;
  private Long amount;
  private Integer action;
  private Integer error;
  private String merchantTransId;
  private String errorNote;
  private String signString;
  private Instant signTime;
}
