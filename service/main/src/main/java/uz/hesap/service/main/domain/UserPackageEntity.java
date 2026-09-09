package uz.hesap.service.main.domain;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;
import uz.hesap.service.main.util.Constants;

// Foydalanuvchi sotib olgan paket. Har shablon bo'yicha limit/usage — user_package_template'da.
@Getter
@Setter
@Table(schema = Constants.SCHEMA, name = Constants.TABLE_USER_PACKAGE)
public class UserPackageEntity {
  @Id private UUID id;
  private UUID packageId;
  private String userIn; // foydalanuvchi PINFL/STIR (userId o'rniga)
  private Instant expDate;
  private Boolean deleted = Boolean.FALSE;
  @CreatedDate private Instant createdAt;
  @LastModifiedDate private Instant updatedAt;
}
