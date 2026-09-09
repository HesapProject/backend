package uz.hesap.service.main.repository;

import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.main.domain.PermissionRequestEntity;
import uz.hesap.service.main.domain.PermissionRequestStatus;

@Repository
public interface PermissionRequestRepository
    extends R2dbcRepository<PermissionRequestEntity, UUID> {

  Flux<PermissionRequestEntity> findByUserToIdAndDeletedIsFalseAndStatusNot(
      UUID userToId, PermissionRequestStatus status, Pageable pageable);

  Mono<Long> countByUserToIdAndDeletedIsFalseAndStatusNot(
      UUID userToId, PermissionRequestStatus status);

  Flux<PermissionRequestEntity> findByUserFromIdAndDeletedIsFalseAndStatusNot(
      UUID userFromId, PermissionRequestStatus status, Pageable pageable);

  Mono<Long> countByUserFromIdAndDeletedIsFalseAndStatusNot(
      UUID userFromId, PermissionRequestStatus status);

  Mono<PermissionRequestEntity> findByIdAndDeletedIsFalse(UUID id);

  Mono<PermissionRequestEntity> findByUserFromIdAndUserToIdAndStatusAndDeletedIsFalse(
      UUID userFromId, UUID userToId, PermissionRequestStatus status);

  Mono<PermissionRequestEntity>
      findFirstByUserFromIdAndUserToIdAndStatusAndDeletedIsFalseOrderByCreatedDateDesc(
          UUID userFromId, UUID userToId, PermissionRequestStatus status);

  Mono<PermissionRequestEntity>
      findFirstByUserFromIdAndUserToIdAndDeletedIsFalseOrderByCreatedDateDesc(
          UUID userFromId, UUID userToId);
}
