package uz.hesap.service.document.domain.document;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.*;
import org.springframework.data.relational.core.mapping.Table;
import uz.hesap.service.document.domain.enums.Currency;
import uz.hesap.service.document.domain.enums.DocumentPartyStatus;
import uz.hesap.service.document.domain.enums.DocumentPurpose;
import uz.hesap.service.document.domain.enums.DocumentStatus;
import uz.hesap.service.document.domain.enums.DocumentType;
import uz.hesap.service.document.util.Constants;

@RequiredArgsConstructor
@Getter
@Setter
@Table(schema = Constants.SCHEMA, name = Constants.TABLE_DOCUMENT)
public class DocumentEntity {
  @Id private UUID id;
  // Taraflar FAQAT PINFL/STIR bilan identifikatsiya qilinadi (in = jismoniy PINFL yoki
  // yuridik STIR). UUID kerak bo'lganda UserServiceClient.getUserByIn orqali tiklanadi.
  private String buyerIn;
  private String sellerIn;
  private UUID templateId;
  private String number;
  private DocumentStatus status;
  private DocumentType type;
  // Hujjat maqsadi — CONTRACT (default) / TTN / AKT / FACTURA.
  private DocumentPurpose purpose = DocumentPurpose.CONTRACT;
  private Double price;
  private Currency currency;
  private UUID currencyId;
  private Double initialPayment;
  private Instant deliveryAt;
  private String documentJson;
  private Boolean deleted = Boolean.FALSE;
  private DocumentPartyStatus buyerStatus;
  private DocumentPartyStatus sellerStatus;
  // Public ko'rish kodi (contract.hesap.uz PIN) — 4 xonali, PDF'dagi QR yonida chiqadi.
  private String accessCode;
  // Generatsiya qilingan PDF cache — fileService'da, tilga qarab ALOHIDA (uz/ru/en bir xil
  // hujjatning turli tilidagi PDF'i, bittasi ikkinchisini almashtira olmaydi).
  // lastModifiedDate'dan kechroq generatsiya qilingan bo'lsa, o'sha til uchun cache valid.
  private String pdfUrl;
  private Instant pdfGeneratedAt;
  private String pdfUrlRu;
  private Instant pdfGeneratedAtRu;
  private String pdfUrlEn;
  private Instant pdfGeneratedAtEn;
  // Yaratuvchi — taraflar kabi PINFL/STIR bilan (UUID emas). Kerakda user-service'dan tiklanadi.
  private String creatorIn;
  @CreatedDate private Instant createdDate;
  @LastModifiedDate private Instant lastModifiedDate;
  @Version private Long version;
}
