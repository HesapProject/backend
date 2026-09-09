package uz.hesap.service.document.domain.document;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.*;
import org.springframework.data.relational.core.mapping.Table;
import uz.hesap.service.document.domain.enums.ClaimStatus;
import uz.hesap.service.document.util.Constants;

@RequiredArgsConstructor
@Getter
@Setter
@Table(schema = Constants.SCHEMA, name = Constants.TABLE_CLAIMS)
public class ClaimEntity {
  @Id private UUID id;
  private UUID contractId;
  // Tomonlar PINFL/STIR — fromIn: yuboruvchi (seller), toIn: qabul qiluvchi (buyer).
  private String fromIn;
  private String toIn;
  private String nameUz;
  private String nameRu;
  private String nameEn;
  private Integer number;
  private ClaimStatus status;
  // template dan klonlangan JSON
  private String templateJson;
  private Boolean deleted = Boolean.FALSE;
  @CreatedBy private UUID createdBy;
  @CreatedDate private Instant createdDate;
  @LastModifiedDate private Instant lastModifiedDate;
}
