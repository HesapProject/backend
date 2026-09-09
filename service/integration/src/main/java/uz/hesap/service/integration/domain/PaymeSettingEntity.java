package uz.hesap.service.integration.domain;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;
import uz.hesap.service.integration.util.Constants;

// Payme to'lov gateway kredensiallari — singleton table (PlumSettingEntity kabi).
@Getter
@Setter
@ToString
@Table(schema = Constants.SCHEMA, name = Constants.TABLE_PAYME_SETTING)
public class PaymeSettingEntity {
  @Id private UUID id;
  private String merchantId;
  private String secret;
  @CreatedDate private Instant createdDate = Instant.now();
  @LastModifiedDate private Instant lastModifiedDate = Instant.now();
}
