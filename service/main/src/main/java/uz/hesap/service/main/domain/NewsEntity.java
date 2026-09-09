package uz.hesap.service.main.domain;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;
import uz.hesap.service.main.util.Constants;

@RequiredArgsConstructor
@Getter
@Setter
@Table(schema = Constants.SCHEMA, name = Constants.TABLE_NEWS)
public class NewsEntity {

  @Id private UUID id;
  private String titleUz;
  private String titleRu;
  private String titleEn;
  private String bodyUz;
  private String bodyRu;
  private String bodyEn;
  private String image;
  private Instant date;
  private Boolean isHome;
  private Boolean isSend;
  private Boolean isDeleted;
  @CreatedDate private Instant createdDate;
  @LastModifiedDate private Instant lastModifiedDate;
}
