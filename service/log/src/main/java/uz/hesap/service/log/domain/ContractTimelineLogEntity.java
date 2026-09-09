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
@Table(schema = Constants.SCHEMA, name = Constants.TABLE_CONTRACT_TIMELINE)
public class ContractTimelineLogEntity {
  @Id private UUID id;
  private UUID contractId;
  private String eventType;
  private String role;
  private String actorIn;
  private Double amount;
  private String currency;
  private Instant occurredAt;
  // Amalni bajargan sessiya va qurilma (audit) — eski yozuvlarda null.
  private UUID sessionId;
  private String device;
  @CreatedDate private Instant createdDate;
}
