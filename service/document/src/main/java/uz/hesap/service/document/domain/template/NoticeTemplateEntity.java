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

// Talabnoma (notice) shabloni — shartnoma shabloniga (contractTemplateId) bog'langan.
// Minimal sxema: contract_template'dagi keraksiz field'lar (taraf nomlari, amount,
// witness_count, valyuta, payment/product flag'lar) bu yerda yo'q.
@RequiredArgsConstructor
@Getter
@Setter
@Table(schema = Constants.SCHEMA, name = Constants.TABLE_NOTICE_TEMPLATE)
public class NoticeTemplateEntity {
  @Id private UUID id;
  // Ota shartnoma shabloni (qaysi shartnoma turi uchun talabnoma shabloni).
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
