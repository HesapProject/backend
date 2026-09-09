package uz.hesap.service.log.domain;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import uz.hesap.service.log.util.Constants;

@RequiredArgsConstructor
@Getter
@Setter
@Table(schema = Constants.SCHEMA, name = Constants.TABLE_ESKIZ_LOG)
public class EskizLogEntity {
  @Id private UUID id;
  private String phone;
  private String content;
  private Boolean isFailed;
  private String error;
  private Instant timestamp;
  // Eskiz'ga ketgan so'rov va undan kelgan javob (JSON, nullable).
  private String request;
  private String response;
  @CreatedDate private Instant createdDate;
}
