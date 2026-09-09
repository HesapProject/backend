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

// hybrid.pochta.uz credential'lari — admin orqali boshqariladigan singleton.
// OAuth2 password grant: username + password -> /token. DB bo'sh bo'lsa yml'ga qaytadi.
@Getter
@Setter
@ToString
@Table(schema = Constants.SCHEMA, name = Constants.TABLE_POCHTA_SETTING)
public class PochtaSettingEntity {
  @Id private UUID id;
  private String baseUrl;
  private String username;
  // Javobga chiqarilmaydi (faqat passwordSet flag).
  private String password;
  @CreatedDate private Instant createdDate = Instant.now();
  @LastModifiedDate private Instant lastModifiedDate = Instant.now();
}
