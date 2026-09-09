package uz.hesap.service.file.domain;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import uz.hesap.service.file.util.Constants;

@RequiredArgsConstructor
@Getter
@Setter
@Table(schema = Constants.SCHEMA, name = Constants.TABLE_CDN_DATA)
public class CdnDataEntity {
  @Id private UUID id;
  private UUID userId;
  private Long contentLength;
  private String ext;
  private String folder;
  private String contentUrl;
  @CreatedDate private Instant createdDate;
}
