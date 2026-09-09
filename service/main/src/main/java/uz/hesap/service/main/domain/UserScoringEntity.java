package uz.hesap.service.main.domain;

import java.time.Instant;
import java.util.UUID;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import uz.hesap.service.main.util.Constants;

// Umumlashgan scoring jurnali — har scoring (Plum va kelajakda boshqalar) bitta qator.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(schema = Constants.SCHEMA, name = "user_scoring")
public class UserScoringEntity {
  @Id private UUID id;
  private String scoringId;    // provider scoring id
  private String scoringType;  // HESAP | KATM | PAYMENT
  private String requesterIn;  // kim so'radi
  private String userIn;       // kim scoring qilindi
  @CreatedDate private Instant createdAt = Instant.now();
}
