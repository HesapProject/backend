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
import uz.hesap.service.document.domain.enums.CancelRequestStatus;
import uz.hesap.service.document.util.Constants;

// Shartnomani bekor qilish so'rovi. Bir taraf yaratadi (requesterIn), ikkinchisi
// tasdiqlaydi (→ shartnoma CANCELLED) yoki rad etadi.
@RequiredArgsConstructor
@Getter
@Setter
@Table(schema = Constants.SCHEMA, name = Constants.TABLE_CANCEL_REQUEST)
public class CancelRequestEntity {
  @Id private UUID id;
  private UUID contractId;
  // Tomonlar PINFL/STIR (shartnomadan); requesterIn — so'rovni kim yaratgani.
  private String buyerIn;
  private String sellerIn;
  private String requesterIn;
  private CancelRequestStatus status;
  private String reason;
  private Boolean deleted = Boolean.FALSE;
  @CreatedDate private Instant createdDate;
  @LastModifiedDate private Instant lastModifiedDate;
}
