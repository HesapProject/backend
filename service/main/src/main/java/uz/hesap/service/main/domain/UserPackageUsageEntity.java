package uz.hesap.service.main.domain;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import uz.hesap.service.main.domain.enums.UserPackageUsageStatus;
import uz.hesap.service.main.util.Constants;

// Paketdan foydalanish jurnali: har bir tuzilgan shartnoma uchun bitta qator.
// Qolgan son = paketdagi grant − shu (user_package_id, template_id) bo'yicha ACTIVE qatorlar.
@Getter
@Setter
@Table(schema = Constants.SCHEMA, name = Constants.TABLE_USER_PACKAGE_USAGE)
public class UserPackageUsageEntity {
  @Id private UUID id;
  private String userIn; // foydalanuvchi PINFL/STIR (userId o'rniga)
  private UUID userPackageId; // user_package.id
  private UUID templateId; // qaysi shablon (shartnoma turi)
  private UUID contractId; // tuzilgan shartnoma (document) id
  private UserPackageUsageStatus status;
  @CreatedDate private Instant createdDate;
}
