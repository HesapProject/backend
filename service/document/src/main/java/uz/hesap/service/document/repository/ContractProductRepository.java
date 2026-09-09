package uz.hesap.service.document.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import uz.hesap.service.document.domain.document.ContractProductEntity;

@Repository
public interface ContractProductRepository
    extends R2dbcRepository<ContractProductEntity, UUID> {
  Flux<ContractProductEntity> findAllByDocumentIdAndDeletedFalse(UUID documentId);
}
