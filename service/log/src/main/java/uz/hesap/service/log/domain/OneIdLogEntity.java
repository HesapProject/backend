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
@Table(schema = Constants.SCHEMA, name = Constants.TABLE_ONE_ID_LOG)
public class OneIdLogEntity {
  @Id private UUID id;
  private UUID userId;
  private String firstName;
  private String lastName;
  private UUID companyId;
  // OneID so'rovi/javobi va holati (JSON/text, nullable).
  private String request;
  private String response;
  private String status;
  private String errorMessage;
  private Instant requestTime;
  @CreatedDate private Instant createdDate;
  @LastModifiedDate private Instant lastModifiedDate;
}
