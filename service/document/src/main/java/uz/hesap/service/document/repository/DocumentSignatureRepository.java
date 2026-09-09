package uz.hesap.service.document.repository;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import uz.hesap.service.document.domain.document.DocumentSignatureEntity;

@Repository
public interface DocumentSignatureRepository
    extends ReactiveCrudRepository<DocumentSignatureEntity, UUID> {

  // Hujjat imzolari — yangidan eskigacha (Log tab uchun).
  Flux<DocumentSignatureEntity> findAllByDocumentIdOrderByCreatedDateDesc(UUID documentId);
}
