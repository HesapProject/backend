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

// KATM (Kredit-axborot tahliliy markazi) kredit byurosi credential'lari —
// admin orqali boshqariladigan singleton table. PlumSettingEntity bilan bir xil
// yondashuv: base_url + login/password DB'da, KatmTokenService JWT token oladi.
@Getter
@Setter
@ToString
@Table(schema = Constants.SCHEMA, name = Constants.TABLE_KATM_SETTING)
public class KatmSettingEntity {
  @Id private UUID id;
  private String baseUrl;
  private String login;
  private String password;
  @CreatedDate private Instant createdDate = Instant.now();
  @LastModifiedDate private Instant lastModifiedDate = Instant.now();
}
