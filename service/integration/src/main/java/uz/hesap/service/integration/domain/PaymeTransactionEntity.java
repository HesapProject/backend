package uz.hesap.service.integration.domain;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import uz.hesap.service.integration.domain.enums.BillingType;

// Billing schema'dagi mavjud `payme` jadvaliga ulanadi (UzCard `billing.plum_cards` kabi).
@Getter
@Setter
@ToString
@Table(schema = "billing", name = "payme")
public class PaymeTransactionEntity {
  @Id private UUID id;

  private String paycomId;
  private Long paycomTime;
  private UUID uuid;
  private BillingType billingType;
  private Long createTime;
  private Long performTime;
  private Long cancelTime;
  private Integer amount;
  private Integer state;
  private Integer reason;

  public PaymeTransactionEntity(
      UUID uuid,
      BillingType billingType,
      Integer amount,
      String paycomId,
      Long paycomTime,
      Integer state,
      Long createTime,
      Long performTime,
      Long cancelTime) {
    this.uuid = uuid;
    this.billingType = billingType;
    this.paycomId = paycomId;
    this.paycomTime = paycomTime;
    this.state = state;
    this.createTime = createTime;
    this.performTime = performTime;
    this.cancelTime = cancelTime;
    this.amount = amount;
  }

  public PaymeTransactionEntity() {}
}
