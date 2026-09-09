package uz.hesap.service.document.domain.document;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import uz.hesap.service.document.domain.enums.SignatureActionType;
import uz.hesap.service.document.util.Constants;

@RequiredArgsConstructor
@Getter
@Setter
@Table(schema = Constants.SCHEMA, name = Constants.TABLE_DOCUMENT_SIGNATURE)
public class DocumentSignatureEntity {
  @Id private UUID id;
  private UUID documentId;
  private UUID userId;
  private String signature;
  private SignatureActionType actionType;
  @CreatedDate private Instant createdDate;
}
