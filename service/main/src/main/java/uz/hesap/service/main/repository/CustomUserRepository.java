package uz.hesap.service.main.repository;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.enums.Role;
import uz.hesap.service.common.util.enums.UserType;
import uz.hesap.service.main.domain.UserEntity;
import uz.hesap.service.main.model.response.AgeBucketCount;

public interface CustomUserRepository {
  // Admin ro'yxati — agregatlar (shartnoma soni/balans/oxirgi tashrif) bilan.
  Flux<AdminUserRow> findByFilter(AdminUserFilter filter, Pageable pageable);

  Mono<Long> countByFilter(AdminUserFilter filter);

  // Mijozlar yosh taqsimoti (PINFL'dan). createdFrom/createdTo — ro'yxatdan o'tgan
  // sana oralig'i (null bo'lsa filtr yo'q, hamma vaqt).
  Flux<AgeBucketCount> clientAgeDistribution(Instant createdFrom, Instant createdTo);

  // Mijozlar jinsi taqsimoti (PINFL'dan). createdFrom/createdTo — sana oralig'i.
  Flux<AgeBucketCount> clientGenderDistribution(Instant createdFrom, Instant createdTo);

  Flux<UserEntity> findEmployees(String search, Role role, Pageable pageable);

  Mono<Long> countEmployees(String search, Role role);

  Flux<UserEntity> searchClients(String searchTerm);
}
