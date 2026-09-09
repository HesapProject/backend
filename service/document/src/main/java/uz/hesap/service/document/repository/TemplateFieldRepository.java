package uz.hesap.service.document.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.document.domain.template.TemplateFieldEntity;
import uz.hesap.service.document.util.Constants;

public interface TemplateFieldRepository extends R2dbcRepository<TemplateFieldEntity, UUID> {
  Flux<TemplateFieldEntity> findAllByTemplateIdAndDeletedFalse(final UUID templateId);

  @Query(
      "update "
          + Constants.SCHEMA
          + "."
          + Constants.TABLE_TEMPLATE_FIELD
          + " set deleted = true, last_modified_date = now()"
          + " where template_id = :templateId"
          + " and deleted = false"
          + " and id <> all(:keepIds)")
  Mono<Integer> softDeleteNotIn(UUID templateId, UUID[] keepIds);
}
