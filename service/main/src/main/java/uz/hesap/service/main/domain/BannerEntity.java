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
import uz.hesap.service.main.util.Constants;

@Getter
@Setter
@ToString
@Table(schema = Constants.SCHEMA, name = Constants.TABLE_BANNER)
public class BannerEntity {
  @Id private UUID id;

  @Column("title_uz")
  private String titleUz;

  @Column("title_ru")
  private String titleRu;

  @Column("title_en")
  private String titleEn;

  private String image;
  private String link;

  @Column("sort_order")
  private Integer sortOrder = 0;

  @Column("is_active")
  private Boolean isActive = Boolean.TRUE;

  @Column("is_deleted")
  private Boolean isDeleted = Boolean.FALSE;

  @CreatedDate private Instant createdDate = Instant.now();
  @LastModifiedDate private Instant lastModifiedDate = Instant.now();
}
