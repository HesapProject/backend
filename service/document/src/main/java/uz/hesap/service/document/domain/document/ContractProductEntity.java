package uz.hesap.service.document.domain.document;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;
import uz.hesap.service.document.domain.enums.ProductUnit;
import uz.hesap.service.document.util.Constants;

// Shartnoma mahsuloti: nom + summa + shablonning product field qiymatlari (JSON).
@Getter
@Setter
@Table(schema = Constants.SCHEMA, name = Constants.TABLE_CONTRACT_PRODUCT)
public class ContractProductEntity {
  @Id private UUID id;
  private UUID documentId;
  private String name;
  private ProductUnit unit; // o'lchov birligi (DONA/KG/LITR)
  private Double price; // birlik narxi (tiyin)
  private Double quantity; // soni
  private Double amount; // jami = price * quantity (tiyin)
  // Topshirish (yetkazish) sanasi — har mahsulot o'z sanasiga ega.
  private Instant deliveryAt;
  // Product field qiymatlari JSON string: {keyName: value}. ('values' SQL reserved.)
  private String fieldValues;
  private Boolean deleted = Boolean.FALSE;
  @CreatedDate private Instant createdDate;
  @LastModifiedDate private Instant lastModifiedDate;
}
