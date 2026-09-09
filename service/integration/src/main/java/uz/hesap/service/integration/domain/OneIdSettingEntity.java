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

// OneID credential'larini admin orqali boshqarish uchun singleton table —
// MyIdSetting/PlumSetting bilan bir xil pattern.
@Getter
@Setter
@ToString
@Table(schema = Constants.SCHEMA, name = Constants.TABLE_ONE_ID_SETTING)
public class OneIdSettingEntity {
  @Id private UUID id;
  private String baseUrl;
  private String clientId;
  private String clientSecret;
  @CreatedDate private Instant createdDate = Instant.now();
  @LastModifiedDate private Instant lastModifiedDate = Instant.now();
}
