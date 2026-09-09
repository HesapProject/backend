package uz.hesap.service.log.domain;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;
import uz.hesap.service.log.util.Constants;

// Rouming (Factura Provider — ЭСФ) draft yuborish loglari (monitoring).
@RequiredArgsConstructor
@Getter
@Setter
@Table(schema = Constants.SCHEMA, name = Constants.TABLE_ROUMING_LOG)
public class RoumingLogEntity {
  @Id private UUID id;
  private UUID contractId;
  private String contractNo;
  private String facturaNo;
  private String sellerTin;
  private String buyerTin;
  private String status;
  private String errorMessage;
  // Rouming'ga ketgan so'rov va undan kelgan javob (JSON, nullable).
  private String request;
  private String response;
  private Instant requestTime;
  @CreatedDate private Instant createdDate;
  @LastModifiedDate private Instant lastModifiedDate;
}
