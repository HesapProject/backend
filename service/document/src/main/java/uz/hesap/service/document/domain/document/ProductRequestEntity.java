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
import uz.hesap.service.document.domain.enums.ProductRequestStatus;
import uz.hesap.service.document.util.Constants;

// Mahsulot so'rovi. Bir taraf yaratadi (requesterIn), ikkinchisi tasdiqlaydi/rad etadi.
// Cancel so'rovidek struktura + qaysi mahsulot haqida ekanini bildiruvchi productId.
@RequiredArgsConstructor
@Getter
@Setter
@Table(schema = Constants.SCHEMA, name = Constants.TABLE_PRODUCT_REQUEST)
public class ProductRequestEntity {
  @Id private UUID id;
  private UUID contractId;
  private UUID productId;
  // Tomonlar PINFL/STIR (shartnomadan); requesterIn — so'rovni kim yaratgani.
  private String buyerIn;
  private String sellerIn;
  private String requesterIn;
  private ProductRequestStatus status;
  private Double amount; // berilgan summa (tiyin) — legacy; endi quantity ishlatiladi
  private Double quantity; // berilgan SONI; product quantity'sidan kam bo'lsa qisman
  private String reason;
  private Boolean deleted = Boolean.FALSE;
  @CreatedDate private Instant createdDate;
  @LastModifiedDate private Instant lastModifiedDate;
}
