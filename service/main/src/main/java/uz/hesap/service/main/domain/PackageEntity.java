package uz.hesap.service.main.domain;

import java.time.Instant;
import java.util.UUID;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;
import uz.hesap.service.main.util.Constants;
import uz.hesap.service.common.util.enums.TariffType;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Table(schema = Constants.SCHEMA, name = Constants.TABLE_PACKAGES)
public class PackageEntity {
  @Id private UUID id;

  private String nameUz;
  private String nameRu;
  private String nameEn;

  private String descriptionUz;
  private String descriptionRu;
  private String descriptionEn;

  private Double price;
  private Integer stars;
  private Integer duration;
  private TariffType type;
  // template config JSON
  private String templates;
  // Per-tur scoring kvotasi (paket beradigan skoring soni). Sarflash keyingi bosqichda.
  private Integer scoringHesap = 0;
  private Integer scoringKatm = 0;
  private Integer scoringPayment = 0;
  private Boolean deleted = Boolean.FALSE;
  @CreatedDate private Instant createdDate;
  @LastModifiedDate private Instant lastModifiedDate;
}
