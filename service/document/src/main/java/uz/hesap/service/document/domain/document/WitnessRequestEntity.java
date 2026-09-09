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
import uz.hesap.service.document.domain.enums.DocumentWitnessStatus;
import uz.hesap.service.document.util.Constants;

// Guvohlik so'rovi. Guvohlikka chaqirilganda shu yerga PENDING bo'lib tushadi;
// qabul qilingach status ACCEPTED bo'ladi va `witnesses` jadvaliga yozuv qo'shiladi.
@RequiredArgsConstructor
@Getter
@Setter
@Table(schema = Constants.SCHEMA, name = Constants.TABLE_WITNESS_REQUEST)
public class WitnessRequestEntity {
  @Id private UUID id;
  private UUID contractId;
  private UUID witnessId;
  private DocumentWitnessStatus status;
  @CreatedDate private Instant createdDate;
  @LastModifiedDate private Instant lastModifiedDate;
}
