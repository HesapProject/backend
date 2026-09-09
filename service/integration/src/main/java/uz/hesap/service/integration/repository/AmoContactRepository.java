package uz.hesap.service.integration.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;
import uz.hesap.service.integration.domain.AmoContactEntity;

public interface AmoContactRepository extends R2dbcRepository<AmoContactEntity, UUID> {
  // Idempotentlik — bir foydalanuvchi uchun ikkinchi marta kontakt yaratmaslik.
  Mono<AmoContactEntity> findFirstByHesapUserId(UUID hesapUserId);
}
