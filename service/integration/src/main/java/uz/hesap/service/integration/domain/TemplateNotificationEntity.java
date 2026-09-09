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
import uz.hesap.service.common.util.message.NotificationEvent;
import uz.hesap.service.integration.util.Constants;

/**
 * Template + hodisa bo'yicha notification sozlamasi. Har (templateId, event) uchun bitta qator;
 * yo'q bo'lsa default (push yoq, sms o'chiq) qo'llaniladi. Document servisdan kelgan
 * ContractNotificationEvent shu config bo'yicha SMS/push'ga aylantiriladi.
 */
@Getter
@Setter
@ToString
@Table(schema = Constants.SCHEMA, name = Constants.TABLE_TEMPLATE_NOTIFICATION)
public class TemplateNotificationEntity {
  @Id private UUID id;
  private UUID templateId;
  private NotificationEvent event;
  private Boolean smsEnabled = Boolean.FALSE;
  private String smsText;
  private Boolean firebaseEnabled = Boolean.TRUE;
  private String firebaseText;
  private Boolean deleted = Boolean.FALSE;
  @CreatedDate private Instant createdDate = Instant.now();
  @LastModifiedDate private Instant lastModifiedDate = Instant.now();
}
