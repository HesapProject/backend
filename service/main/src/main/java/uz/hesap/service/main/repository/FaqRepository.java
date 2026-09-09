package uz.hesap.service.main.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.main.domain.FaqEntity;

/**
 * Reactive repository for FAQ entity operations. Provides CRUD operations with soft delete support.
 *
 * @author elmurod
 * @since 2026-02-07
 */
@Repository
public interface FaqRepository extends R2dbcRepository<FaqEntity, UUID> {

  /**
   * Finds a single FAQ by ID that is not deleted.
   *
   * @param id the FAQ UUID
   * @return Mono containing the FAQ if found and not deleted
   */
  Mono<FaqEntity> findByIdAndIsDeletedFalse(UUID id);

  /**
   * Finds all FAQs that are not deleted, ordered by creation date descending. Returns the most
   * recently created FAQs first.
   *
   * @return Flux of all active FAQs
   */
  @Query("SELECT * FROM \"user\".faq WHERE is_deleted = FALSE ORDER BY created_date DESC")
  Flux<FaqEntity> findAllByIsDeletedFalseOrderByCreatedDateDesc();

  /**
   * Counts all FAQs that are not deleted.
   *
   * @return Mono containing the count of active FAQs
   */
  Mono<Long> countByIsDeletedFalse();
}
