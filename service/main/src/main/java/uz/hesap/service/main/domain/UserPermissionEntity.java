package uz.hesap.service.main.domain;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;
import uz.hesap.service.main.util.Constants;

@Getter
@Setter
@ToString
@Table(schema = Constants.SCHEMA, name = Constants.TABLE_USER_PERMISSION)
public class UserPermissionEntity {
  @Id private UUID id;
  // Foydalanuvchilar IN (jismoniy PINFL / yuridik STIR) bo'yicha bog'lanadi —
  // user yangi DB'da bo'lmasa ham ruxsat saqlanadi (legacy migratsiya uchun).
  private String userFromIn;
  private String userToIn;
  private Boolean userInfo = Boolean.FALSE;
  private Boolean passport = Boolean.FALSE;
  private Boolean payability = Boolean.FALSE;
  private Boolean contract = Boolean.FALSE;
  private Boolean partner = Boolean.FALSE;
  private Boolean active = Boolean.TRUE;
  private Boolean deleted = Boolean.FALSE;
  @CreatedDate private Instant createdDate;
  @LastModifiedDate private Instant lastModifiedDate;
}
