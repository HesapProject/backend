package uz.hesap.service.integration.promos;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Table(schema = "\"user\"", name = "promos")
public class PromosEntity {
  @Id private UUID id;

  private String code;
  // Chegirma turi — foiz (PERCENT) yoki aniq summa (FIXED).
  private PromosDiscountType discountType;
  // Chegirma miqdori — discountType'ga ko'ra foiz yoki so'mdagi summa.
  private Double discountAmount;
  // Ishlatilish turi: bir martalik / ko'p kishiga bir martalik / ko'p kishiga ko'p martalik.
  private PromosUsageType usageType;
  // Auditoriya qamrovi (yangi/18-25...) — detal mezonlar keyin ishlab chiqiladi.
  private PromosAudience audience = PromosAudience.ALL;
  // Amal qilish muddati.
  private LocalDate validFrom;
  private LocalDate validTo;
  private Boolean active = Boolean.TRUE;
  private boolean deleted = false;
  @CreatedDate private Instant createdDate;
  @LastModifiedDate private Instant lastModifiedDate;
}
