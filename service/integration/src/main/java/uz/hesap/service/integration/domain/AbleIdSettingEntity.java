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

// AbleID credential'larini admin orqali boshqarish uchun singleton table —
// PlumSettingEntity bilan bir xil yondashuv. hookUrl — webhook qabul qiladigan
// bizning public endpoint (AbleID sessiya yaratishda hooks[] sifatida yuboriladi).
@Getter
@Setter
@ToString
@Table(schema = Constants.SCHEMA, name = Constants.TABLE_ABLE_ID_SETTING)
public class AbleIdSettingEntity {
  @Id private UUID id;
  private String baseUrl;
  private String projectId;
  private String secret;
  private String hookUrl;
  @CreatedDate private Instant createdDate = Instant.now();
  @LastModifiedDate private Instant lastModifiedDate = Instant.now();
}
