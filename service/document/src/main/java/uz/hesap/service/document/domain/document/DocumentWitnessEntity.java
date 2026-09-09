package uz.hesap.service.document.domain.document;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;
import uz.hesap.service.document.util.Constants;

// Qabul qilingan guvohlar (witness_requests qabul qilingach shu yerga yoziladi).
// Status YO'Q — bu jadvalda faqat ACCEPTED guvohlar bo'ladi (mavjudlik = qabul qilingan).
@RequiredArgsConstructor
@Getter
@Setter
@Table(schema = Constants.SCHEMA, name = Constants.TABLE_CONTRACT_WITNESS)
public class DocumentWitnessEntity {
  @Id private UUID id;
  private UUID contractId;
  private UUID witnessId;
  @CreatedDate private Instant createdDate;
  @LastModifiedDate private Instant lastModifiedDate;
}
