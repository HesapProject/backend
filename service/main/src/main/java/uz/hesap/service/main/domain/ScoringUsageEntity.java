package uz.hesap.service.main.domain;

import java.time.Instant;
import java.util.UUID;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import uz.hesap.service.main.util.Constants;

// Scoring ishlatilishi — har scoring paketning per-tur kvotasidan hisoblanadi.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(schema = Constants.SCHEMA, name = "scoring_usage")
public class ScoringUsageEntity {
  @Id private UUID id;
  private String userIn;
  private UUID packageId;
  private String scoringType; // HESAP | KATM | PAYMENT
  private String scoringRef;
  @CreatedDate private Instant createdAt = Instant.now();
}
