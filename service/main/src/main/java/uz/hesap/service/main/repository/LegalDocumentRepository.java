package uz.hesap.service.main.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;
import uz.hesap.service.main.domain.LegalDocumentEntity;
import uz.hesap.service.main.domain.LegalDocumentType;

public interface LegalDocumentRepository extends R2dbcRepository<LegalDocumentEntity, UUID> {
  Mono<LegalDocumentEntity> findFirstByTypeOrderByCreatedDateDesc(LegalDocumentType type);
}
