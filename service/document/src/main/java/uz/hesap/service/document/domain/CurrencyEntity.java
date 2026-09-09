package uz.hesap.service.document.domain;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;
import uz.hesap.service.document.util.Constants;

@RequiredArgsConstructor
@Getter
@Setter
@Table(schema = Constants.SCHEMA, name = Constants.TABLE_CURRENCY)
public class CurrencyEntity {
  @Id private UUID id;
  private String code;
  private String nameUz;
  private String nameRu;
  private String nameEn;
  private String symbol;
  private Integer sortOrder = 0;
  private Boolean isActive = Boolean.TRUE;
  private Boolean deleted = Boolean.FALSE;
  @CreatedDate private Instant createdDate;
  @LastModifiedDate private Instant lastModifiedDate;
  @Version private Long version;
}
