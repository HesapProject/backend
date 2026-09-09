package uz.hesap.service.document.repository;

import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.document.domain.document.DocumentEntity;

public interface DocumentRepository extends R2dbcRepository<DocumentEntity, UUID> {
  // DIQQAT: taraf (buyer_in/seller_in) bo'yicha derived query YOZMA — Spring Data 'In'ni
  // IN-keyword deb o'qiydi (seller IN ...) → startup'da yiqiladi. CustomDocumentRepository
  // .findFiltered/.countFiltered ishlat (raw SQL: buyer_in = :in OR seller_in = :in).

  Mono<DocumentEntity> findByIdAndDeletedFalse(final UUID id);
}
