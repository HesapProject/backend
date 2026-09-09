package uz.hesap.service.main.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import uz.hesap.service.main.domain.StoryViewEntity;

@Repository
public interface StoryViewRepository extends R2dbcRepository<StoryViewEntity, UUID> {

  @Modifying
  @Query(
      """
          INSERT INTO \"user\".story_view ( story_id, user_id, viewed_at)
          VALUES ( :storyId, :userId, NOW())
          ON CONFLICT (story_id, user_id) DO NOTHING
          """)
  Mono<Void> insertIfNotExists(UUID storyId, UUID userId);
}
