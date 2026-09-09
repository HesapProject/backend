package uz.hesap.service.main.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.main.domain.StaffEntity;
import uz.hesap.service.main.domain.enums.StaffStatus;
import uz.hesap.service.main.domain.enums.StaffType;

public interface StaffRepository extends R2dbcRepository<StaffEntity, UUID> {

  Flux<StaffEntity> findAllByDeletedFalseOrderByCreatedDateDesc();

  // So'rovlar: foydalanuvchiga kelganlar (user_id) va aktor yuborganlar (created_by).
  Flux<StaffEntity> findAllByUserIdAndDeletedFalseOrderByCreatedDateDesc(UUID userId);

  Flux<StaffEntity> findAllByCreatedByAndDeletedFalseOrderByCreatedDateDesc(UUID createdBy);

  // OneID yuridik login: person↔company OWNER bog'lanishini dedup qilish uchun.
  Mono<StaffEntity> findFirstByUserIdAndCompanyIdAndDeletedFalse(UUID userId, UUID companyId);

  // getMe: foydalanuvchining aktiv (qabul qilingan) birinchi staff bog'lanishi.
  Mono<StaffEntity> findFirstByUserIdAndStatusAndDeletedFalseOrderByCreatedDateAsc(
      UUID userId, StaffStatus status);

  // getCompanyByInn: kompaniyaning OWNER egasi.
  Mono<StaffEntity> findFirstByCompanyIdAndTypeAndStatusAndDeletedFalse(
      UUID companyId, StaffType type, StaffStatus status);
}
