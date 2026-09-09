package uz.hesap.service.main.domain;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import uz.hesap.service.common.util.enums.Permission;
import uz.hesap.service.main.domain.enums.StaffStatus;
import uz.hesap.service.main.domain.enums.StaffType;
import uz.hesap.service.main.util.Constants;

// Staff: kompaniya (company_id) foydalanuvchini (user_id) xodimlikka taklif qiladi —
// type (OWNER/MANAGER), permissions (Permission[] enum) va status (PENDING/ACCEPTED/REJECTED).
// Eski white_list_type jadvalidan qayta qurilgan.
@Getter
@Setter
@ToString
@Table(schema = Constants.SCHEMA, name = "staff")
public class StaffEntity {
  @Id private UUID id;

  // user — xodimlikka taklif qilingan foydalanuvchi.
  @Column("user_id")
  private UUID userId;

  // company — taklif qiluvchi kompaniya.
  @Column("company_id")
  private UUID companyId;

  private StaffType type;

  // So'rov holati: PENDING (kutmoqda) / ACCEPTED (xodim) / REJECTED.
  private StaffStatus status = StaffStatus.PENDING;

  // Ruxsatlar — Permission enum massivi.
  private Permission[] permissions;

  @Column("created_by")
  private UUID createdBy;

  @Column("updated_by")
  private UUID updatedBy;

  @CreatedDate
  @Column("created_date")
  private Instant createdDate;

  @LastModifiedDate
  @Column("last_modified_date")
  private Instant lastModifiedDate;

  private Boolean deleted = Boolean.FALSE;
}
