package uz.hesap.service.integration.domain;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import uz.hesap.service.integration.util.Constants;

// hybrid.pochta.uz so'rovlari tarixi (monitoring). request/response — TEXT.
@Getter
@Setter
@ToString
@Table(schema = Constants.SCHEMA, name = Constants.TABLE_POCHTA_LOG)
public class PochtaLogEntity {
  @Id private UUID id;
  private String action;
  private String status;
  private String mailId;
  private String request;
  private String response;
  private String errorMessage;
  private Instant createdAt = Instant.now();
}
