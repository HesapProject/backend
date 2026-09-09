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

@Getter
@Setter
@ToString
@Table(schema = Constants.SCHEMA, name = Constants.TABLE_ESKIZ_SETTING)
public class SmsProviderSettingEntity {
  @Id private UUID id;
  private String eskizEmail;
  private String eskizSecret;
  // SMS yuboruvchi nickname (Eskiz'da tasdiqlangan, masalan "HESAP"). Bo'sh bo'lsa "4546".
  private String eskizFrom;
  @CreatedDate private Instant createdDate = Instant.now();
  @LastModifiedDate private Instant lastModifiedDate = Instant.now();
}
