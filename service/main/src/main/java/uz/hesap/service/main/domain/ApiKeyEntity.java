package uz.hesap.service.main.domain;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;
import uz.hesap.service.main.util.Constants;

// OpenAPI kaliti. Kalitning o'zi hech qachon saqlanmaydi — faqat SHA-256 hash
// (keyHash) va ko'rsatish uchun boshlanish qismi (keyPrefix).
@Getter
@Setter
@Table(schema = Constants.SCHEMA, name = Constants.TABLE_API_KEY)
public class ApiKeyEntity {

  @Id private UUID id;
  // Egasining barqaror identifikatori: jismoniy uchun PINFL, yuridik uchun STIR.
  private String ownerIn;
  private UUID ownerUserId;
  private String name;
  private String keyPrefix;
  private String keyHash;
  // ApiScope nomlari.
  private String[] scopes;
  // true -> barcha shablonlar (full), aks holda templateIds.
  private Boolean allTemplates = Boolean.FALSE;
  private UUID[] templateIds;
  // NULL -> muddatsiz (abadiy).
  private Instant expiresAt;
  private String webhookUrl;
  private String webhookSecret;
  // WebhookEventType nomlari; bo'sh/NULL -> barcha hodisalar.
  private String[] webhookEvents;
  private Boolean active = Boolean.TRUE;
  private Instant revokedAt;
  private Instant lastUsedAt;
  private Boolean deleted = Boolean.FALSE;
  @CreatedDate private Instant createdDate;
  @LastModifiedDate private Instant lastModifiedDate;

  // Kalit hozir ishlatishga yaroqlimi: aktiv, o'chirilmagan, muddati o'tmagan.
  public boolean usable() {
    if (!Boolean.TRUE.equals(active) || Boolean.TRUE.equals(deleted)) {
      return false;
    }
    return expiresAt == null || expiresAt.isAfter(Instant.now());
  }
}
