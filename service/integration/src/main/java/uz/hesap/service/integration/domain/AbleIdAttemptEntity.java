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

// AbleID sessiya urinishi. Webhook FAQAT muvaffaqiyatda keladi — shunda status
// SUCCESS bo'ladi; aks holda PENDING qoladi (sessiya 10 daqiqada eskiradi).
@Getter
@Setter
@ToString
@Table(schema = Constants.SCHEMA, name = Constants.TABLE_ABLE_ID_ATTEMPT)
public class AbleIdAttemptEntity {
  @Id private UUID id;
  private String attemptId; // AbleID sessiya identifikatori
  private String transactionId; // biz yaratgan unikal tranzaksiya id
  private String userIn; // sessiya ochilgan foydalanuvchi PINFL
  private String status; // PENDING | SUCCESS
  private String payload; // webhook'dan kelgan data (JSON, audit uchun)
  @CreatedDate private Instant createdDate = Instant.now();
  @LastModifiedDate private Instant lastModifiedDate = Instant.now();
}
