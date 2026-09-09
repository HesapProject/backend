package uz.hesap.service.main.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.main.domain.StoryEntity;

@Repository
public interface StoryRepository extends R2dbcRepository<StoryEntity, UUID> {

  Mono<StoryEntity> findByIdAndIsDeletedFalse(UUID id);

  Flux<StoryEntity> findAllByIsDeletedFalseOrderByCreatedDateDesc();

  @Query(
      """
            SELECT s.id, s.title_uz, s.title_ru, s.title_en, s.avatar, s.items, s.created_date,
                   CASE WHEN sv.id IS NOT NULL THEN TRUE ELSE FALSE END AS is_viewed
            FROM \"user\".story s
            LEFT JOIN \"user\".story_view sv ON s.id = sv.story_id AND sv.user_id = :userId
            WHERE s.is_deleted = FALSE
            ORDER BY s.created_date DESC
            """)
  Flux<StoryProjection> findAllWithViewStatus(UUID userId);

  interface StoryProjection {
    UUID getId();

    String getTitleUz();

    String getTitleRu();

    String getTitleEn();

    String getAvatar();

    String getItems();

    java.time.Instant getCreatedDate();

    Boolean getIsViewed();
  }
}
