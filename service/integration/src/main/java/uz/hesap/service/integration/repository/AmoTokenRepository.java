package uz.hesap.service.integration.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;
import uz.hesap.service.integration.domain.AmoTokenEntity;

public interface AmoTokenRepository extends R2dbcRepository<AmoTokenEntity, UUID> {
  // Oxirgi (eng yangi) token — getAccessToken shundan foydalanadi.
  Mono<AmoTokenEntity> findTopByOrderByCreatedDateDesc();
}
