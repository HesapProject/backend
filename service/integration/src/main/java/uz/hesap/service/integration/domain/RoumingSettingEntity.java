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

// Factura.uz (elektron schyot-faktura/ЭСФ) credential'lari — OAuth2 password grant.
// baseUrl = API bazasi (https://api.faktura.uz), tokenUrl = token endpoint
// (https://account.faktura.uz/token). login/password = akkaunt username/parol,
// clientId/clientSecret = ilova credential'lari. Admin boshqaradigan singleton;
// DB qatori bo'sh bo'lsa application.yml qiymatlariga qaytadi.
@Getter
@Setter
@ToString
@Table(schema = Constants.SCHEMA, name = Constants.TABLE_ROUMING_SETTING)
public class RoumingSettingEntity {
  @Id private UUID id;
  private String baseUrl;
  // Akkaunt username (Factura.uz login). Eski `login` ustuni qayta ishlatiladi.
  private String login;
  // Akkaunt paroli — javobga chiqarilmaydi (faqat passwordSet flag).
  private String password;
  // OAuth2 ilova credential'lari.
  private String clientId;
  // clientSecret — javobga chiqarilmaydi (faqat clientSecretSet flag).
  private String clientSecret;
  // Token endpoint (https://account.faktura.uz/token). NULL bo'lsa yml default.
  private String tokenUrl;
  @CreatedDate private Instant createdDate = Instant.now();
  @LastModifiedDate private Instant lastModifiedDate = Instant.now();
}
