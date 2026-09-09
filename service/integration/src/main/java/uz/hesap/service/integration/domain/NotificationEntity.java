package uz.hesap.service.integration.domain;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;
import uz.hesap.service.common.util.message.NotificationType;
import uz.hesap.service.integration.util.Constants;

@Getter
@Setter
@ToString
@Table(schema = Constants.SCHEMA, name = Constants.TABLE_NOTIFICATION)
public class NotificationEntity {
  @Id private UUID id;
  private UUID dataId;
  private NotificationType type;
  private UUID userId;

  private String titleUz;
  private String titleRu;
  private String titleEn;

  private String bodyUz;
  private String bodyRu;
  private String bodyEn;

  private String image;
  private Boolean isViewed = Boolean.FALSE;
  private Boolean deleted = Boolean.FALSE;
  @CreatedBy private UUID createdUserId;
  @CreatedDate private Instant createdAt = Instant.now();
  @LastModifiedDate private Instant updatedAt = Instant.now();
}
