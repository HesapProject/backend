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

@RequiredArgsConstructor
@Getter
@Setter
@Table(schema = Constants.SCHEMA, name = Constants.TABLE_PAYME_LOG)
public class PaymeLogEntity {
  @Id private UUID id;
  private UUID userId;
  private UUID companyId;
  private String type;
  private String status;
  private String errorMessage;
  // Payme'ga ketgan so'rov va undan kelgan javob (JSON, nullable).
  private String request;
  private String response;
  private Instant requestTime;
  @CreatedDate private Instant createdDate;
  @LastModifiedDate private Instant lastModifiedDate;
}
