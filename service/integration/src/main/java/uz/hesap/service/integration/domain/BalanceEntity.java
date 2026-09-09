package uz.hesap.service.integration.domain;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;
import uz.hesap.service.integration.domain.enums.BalanceType;
import uz.hesap.service.integration.domain.enums.BillingType;

// Billing schema'dagi mavjud `balance` jadvaliga ulanadi. Payme/Click to'lovi
// muvaffaqiyatli bo'lganda balans shu yerda to'ldiriladi (@Version optimistik lock).
// DIQQAT: `user.balance` jadvali prod'da yaratilmagan (billing→user migratsiyasi
// qisman qolgan) — jonli balans `billing.balance`da, shu sabab shu schema.
@Getter
@Setter
@ToString
@Table(schema = "billing", name = "balance")
public class BalanceEntity {
  @Id private UUID id;
  private UUID uniqueId;
  private Double balance;
  private BalanceType balanceType;
  private BillingType billingType;
  private Instant expireDate;
  private UUID tariffId;
  private Boolean deleted = Boolean.FALSE;
  private Instant activationDate;
  @CreatedDate private Instant createdDate;
  @LastModifiedDate private Instant lastModifiedDate;
  @Version private Long version;
}
