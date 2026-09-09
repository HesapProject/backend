package uz.hesap.service.main.domain;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import uz.hesap.service.main.util.Constants;

@RequiredArgsConstructor
@Getter
@Setter
@Table(schema = Constants.SCHEMA, name = Constants.TABLE_STORY_VIEW)
public class StoryViewEntity {

  @Id private UUID id;
  private UUID storyId;
  private UUID userId;
  @CreatedDate private Instant viewedAt;
}
