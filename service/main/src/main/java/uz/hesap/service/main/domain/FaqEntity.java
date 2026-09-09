package uz.hesap.service.main.domain;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;
import uz.hesap.service.main.util.Constants;

@RequiredArgsConstructor
@Getter
@Setter
@Table(schema = Constants.SCHEMA, name = Constants.TABLE_FAQ)
public class FaqEntity {

  @Id private UUID id;

  private String titleUz;
  private String titleRu;
  private String titleEn;
  private String answerUz;
  private String answerRu;
  private String answerEn;
  private Boolean isDeleted = Boolean.FALSE;

  @CreatedDate private Instant createdDate;

  @LastModifiedDate private Instant lastModifiedDate;
}
