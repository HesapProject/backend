package uz.hesap.service.log.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;
import uz.hesap.service.common.util.enums.UserLogReason;
import uz.hesap.service.log.util.Constants;

@RequiredArgsConstructor
@Getter
@Setter
@Table(schema = Constants.SCHEMA, name = Constants.TABLE_USER_LOG)
public class UserLogEntity {
  @Id private UUID id;
  private UUID userId;
  private UserLogReason reason;
  private String firstName;
  private String lastName;
  private Map<String, Object> oldVersion;
  private Map<String, Object> newVersion;
  private List<String> differentFields;
  @CreatedDate private Instant createdDate;
  @LastModifiedDate private Instant lastModifiedDate;
}
