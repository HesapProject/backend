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

// Turanix partnyor-servis credential'lari — admin orqali boshqariladigan singleton.
// HMAC-SHA256 imzo uchun project_id + secret_key kerak. KatmSetting bilan bir xil
// yondashuv: DB qatori bo'sh bo'lsa application.yml qiymatlariga qaytadi.
@Getter
@Setter
@ToString
@Table(schema = Constants.SCHEMA, name = Constants.TABLE_TURANIX_SETTING)
public class TuranixSettingEntity {
  @Id private UUID id;
  private String baseUrl;
  private String projectId;
  // HMAC imzo kaliti — javobga chiqarilmaydi (faqat secretSet flag).
  private String secretKey;
  @CreatedDate private Instant createdDate = Instant.now();
  @LastModifiedDate private Instant lastModifiedDate = Instant.now();
}
