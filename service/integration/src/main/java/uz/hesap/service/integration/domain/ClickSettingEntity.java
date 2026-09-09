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

// Click to'lov gateway kredensiallari — singleton table (PlumSettingEntity kabi).
@Getter
@Setter
@ToString
@Table(schema = Constants.SCHEMA, name = Constants.TABLE_CLICK_SETTING)
public class ClickSettingEntity {
  @Id private UUID id;
  private Long serviceId;
  private String merchantId;
  private String secretKey;
  @CreatedDate private Instant createdDate = Instant.now();
  @LastModifiedDate private Instant lastModifiedDate = Instant.now();
}
