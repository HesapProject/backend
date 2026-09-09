package uz.hesap.service.document.repository;

import java.util.Collection;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import uz.hesap.service.document.domain.document.DocumentValueEntity;

public interface DocumentValueRepository extends R2dbcRepository<DocumentValueEntity, UUID> {
  Flux<DocumentValueEntity> findAllByDocumentIdAndDeletedFalse(final UUID documentId);

  // batch: ko'p documents uchun values olish — N+1 oldini oladi
  Flux<DocumentValueEntity> findAllByDocumentIdInAndDeletedFalse(
      final Collection<UUID> documentIds);
}
