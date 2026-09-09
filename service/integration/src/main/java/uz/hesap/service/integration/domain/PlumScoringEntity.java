package uz.hesap.service.integration.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import uz.hesap.service.integration.domain.enums.ScoringStatus;
import uz.hesap.service.integration.domain.enums.ScoringType;
import uz.hesap.service.integration.util.Constants;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(schema = Constants.SCHEMA, name = Constants.TABLE_PLUM_SCORING)
public class PlumScoringEntity {
  @Id private UUID id;
  // Karta egasi PINFL/STIR (user UUID o'rniga) — saqlash/olish PINFL bo'yicha.
  private String userIn;
  // Skoringni SO'RAGAN foydalanuvchi PINFL/STIR — faqat unga ko'rsatish uchun.
  private String requesterIn;
  private UUID cardId;
  private Instant createdAt;
  private ScoringType type;
  private Map<String, Object> response;
  private Map<String, Object> uzcard;
  private Map<String, Object> humo;
  private Integer plumScoringId;
  private ScoringStatus status;
}
