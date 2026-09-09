package uz.hesap.service.document.domain.template;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;
import uz.hesap.service.document.domain.enums.TemplateStatus;
import uz.hesap.service.document.domain.enums.VerificationType;
import uz.hesap.service.document.util.Constants;

// Da'vo arizasi (claim) shabloni — shartnoma shabloniga (contractTemplateId) bog'langan.
// NoticeTemplateEntity bilan bir xil minimal sxema.
@RequiredArgsConstructor
@Getter
@Setter
@Table(schema = Constants.SCHEMA, name = Constants.TABLE_CLAIM_TEMPLATE)
public class ClaimTemplateEntity {
  @Id private UUID id;
  private UUID contractTemplateId;
  private UUID companyId;
  private String nameUz;
  private String nameRu;
  private String nameEn;
  private String templateData;
  private String templateStructure;
  private TemplateStatus status;
  private VerificationType individualVerificationType;
  private VerificationType legalVerificationType;
  private Boolean deleted = Boolean.FALSE;
  @CreatedDate private Instant createdDate;
  @LastModifiedDate private Instant lastModifiedDate;
}
