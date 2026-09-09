package uz.hesap.service.main.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;
import uz.hesap.service.main.domain.NewsEntity;

public interface NewsRepository extends R2dbcRepository<NewsEntity, UUID>, CustomNewsRepository {
  Mono<NewsEntity> findByIdAndIsDeletedFalse(UUID id);
}
