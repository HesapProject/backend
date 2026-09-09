package uz.hesap.service.integration.domain;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import uz.hesap.service.integration.util.Constants;

// amoCRM OAuth2 tokeni (eski crm-app amo.tokens_table o'rniga). Har yangilashda
// yangi qator yoziladi; getAccessToken oxirgi (created_date DESC) tokenни oladi va
// muddati o'tsa refresh_token bilan avtoyangilaydi.
@Getter
@Setter
@ToString
@Table(schema = Constants.SCHEMA, name = Constants.TABLE_AMO_TOKEN)
public class AmoTokenEntity {
  @Id private UUID id;
  private String clientId;
  private String clientSecret;
  private String redirectUri;
  private String accessToken;
  private String refreshToken;
  // amoCRM javobidagi server_time (epoch soniya) va expires_in (soniya).
  private Long serverTime;
  private Long expiresIn;
  private String tokenType;
  @CreatedDate private Instant createdDate = Instant.now();
}
