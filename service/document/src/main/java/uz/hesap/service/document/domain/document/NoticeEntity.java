package uz.hesap.service.document.domain.document;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.*;
import org.springframework.data.relational.core.mapping.Table;
import uz.hesap.service.document.domain.enums.NoticeStatus;
import uz.hesap.service.document.util.Constants;

@RequiredArgsConstructor
@Getter
@Setter
@Table(schema = Constants.SCHEMA, name = Constants.TABLE_NOTICE)
public class NoticeEntity {
  @Id private UUID id;
  private UUID contractId;
  // Tomonlar PINFL/STIR (shartnoma buyer_in/seller_in'idan) — UUID saqlanmaydi.
  private String buyerIn;
  private String sellerIn;
  private String nameUz;
  private String nameRu;
  private String nameEn;
  private Integer number;
  private NoticeStatus status;
  private String templateJson;
  private Boolean deleted = Boolean.FALSE;
  @CreatedBy private UUID createdBy;
  @CreatedDate private Instant createdDate;
  @LastModifiedDate private Instant lastModifiedDate;
}
