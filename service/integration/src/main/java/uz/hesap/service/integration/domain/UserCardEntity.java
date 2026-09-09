package uz.hesap.service.integration.domain;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;
import uz.hesap.service.integration.domain.enums.CardType;

// Cross-schema reference: plum_cards billing schema'da turibdi va u yerda
// boshqariladi (Plum karta to'lovlari). Plum scoring shu jadvalga ham ulanadi.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(schema = "billing", name = "plum_cards")
public class UserCardEntity {
  @Id private UUID id;
  // Karta egasi PINFL/STIR (user UUID o'rniga). UUID kerakda resolveUserIdByPinfl orqali.
  private String userIn;
  private Long cardId;
  private String cardNumber;
  private String expireDate;
  private CardType type;
  private Integer status;
  private Integer session;

  @CreatedDate private Instant createdDate;
  @LastModifiedDate private Instant lastModifiedDate;
}
