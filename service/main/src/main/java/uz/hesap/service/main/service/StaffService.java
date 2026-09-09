package uz.hesap.service.main.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.exception.ForbiddenException;
import uz.hesap.service.common.exception.NotFoundException;
import uz.hesap.service.common.util.enums.Permission;
import uz.hesap.service.main.domain.StaffEntity;
import uz.hesap.service.main.domain.UserEntity;
import uz.hesap.service.main.domain.enums.StaffStatus;
import uz.hesap.service.main.model.request.StaffRequest;
import uz.hesap.service.main.model.response.StaffResponse;
import uz.hesap.service.main.repository.StaffRepository;
import uz.hesap.service.main.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class StaffService {

  private final StaffRepository staffRepository;
  private final UserRepository userRepository;

  // Joriy foydalanuvchining staff a'zoliklari (o'zi bo'lgan kompaniyalar) — BARCHA
  // staff'ni emas (aks holda switcher hammaning kompaniyalarini ko'rsatardi).
  public Flux<StaffResponse> list(final UUID userId) {
    return staffRepository
        .findAllByUserIdAndDeletedFalseOrderByCreatedDateDesc(userId)
        .collectList()
        .flatMapMany(this::enrich);
  }

  public Mono<StaffResponse> getById(UUID id) {
    return staffRepository
        .findById(id)
        .filter(s -> !Boolean.TRUE.equals(s.getDeleted()))
        .switchIfEmpty(Mono.error(new NotFoundException("Staff topilmadi: " + id)))
        .flatMap(this::enrichOne);
  }

  // To'g'ridan-to'g'ri qo'shish (admin) — darhol ACCEPTED.
  public Mono<StaffResponse> create(StaffRequest req, UUID actorId) {
    return saveStaff(req, actorId, StaffStatus.ACCEPTED).flatMap(this::enrichOne);
  }

  public Mono<StaffResponse> update(UUID id, StaffRequest req, UUID actorId) {
    return staffRepository
        .findById(id)
        .switchIfEmpty(Mono.error(new NotFoundException("Staff topilmadi: " + id)))
        .flatMap(
            e -> {
              if (req.type() != null) e.setType(req.type());
              if (req.companyId() != null) e.setCompanyId(req.companyId());
              if (req.userId() != null) e.setUserId(req.userId());
              if (req.permissions() != null) e.setPermissions(toArray(req.permissions()));
              e.setUpdatedBy(actorId);
              return staffRepository.save(e);
            })
        .flatMap(this::enrichOne);
  }

  public Mono<Void> delete(UUID id) {
    return staffRepository
        .findById(id)
        .switchIfEmpty(Mono.error(new NotFoundException("Staff topilmadi: " + id)))
        .flatMap(
            e -> {
              e.setDeleted(Boolean.TRUE);
              return staffRepository.save(e);
            })
        .then();
  }

  // ===== So'rov oqimi (StaffRequestController) =====

  // Kompaniya foydalanuvchini xodimlikka taklif qiladi — PENDING so'rov.
  public Mono<StaffResponse> createRequest(StaffRequest req, UUID actorId) {
    return saveStaff(req, actorId, StaffStatus.PENDING).flatMap(this::enrichOne);
  }

  // Foydalanuvchiga kelgan takliflar (user_id = userId).
  public Flux<StaffResponse> incomingRequests(UUID userId) {
    return staffRepository
        .findAllByUserIdAndDeletedFalseOrderByCreatedDateDesc(userId)
        .collectList()
        .flatMapMany(this::enrich);
  }

  // Aktor yuborgan takliflar (created_by = actorId).
  public Flux<StaffResponse> outgoingRequests(UUID actorId) {
    return staffRepository
        .findAllByCreatedByAndDeletedFalseOrderByCreatedDateDesc(actorId)
        .collectList()
        .flatMapMany(this::enrich);
  }

  // Foydalanuvchi taklifni qabul qiladi -> ACCEPTED (xodim bo'ladi).
  public Mono<StaffResponse> acceptRequest(UUID id, UUID userId) {
    return changeStatus(id, userId, StaffStatus.ACCEPTED);
  }

  // Foydalanuvchi taklifni rad etadi -> REJECTED.
  public Mono<StaffResponse> rejectRequest(UUID id, UUID userId) {
    return changeStatus(id, userId, StaffStatus.REJECTED);
  }

  // Kompaniya o'z yuborgan taklifini bekor qiladi (soft delete).
  public Mono<Void> cancelRequest(UUID id) {
    return staffRepository
        .findById(id)
        .filter(s -> !Boolean.TRUE.equals(s.getDeleted()))
        .switchIfEmpty(Mono.error(new NotFoundException("Staff so'rovi topilmadi: " + id)))
        .flatMap(
            e -> {
              e.setDeleted(Boolean.TRUE);
              return staffRepository.save(e);
            })
        .then();
  }

  private Mono<StaffResponse> changeStatus(UUID id, UUID userId, StaffStatus status) {
    return staffRepository
        .findById(id)
        .filter(s -> !Boolean.TRUE.equals(s.getDeleted()))
        .switchIfEmpty(Mono.error(new NotFoundException("Staff so'rovi topilmadi: " + id)))
        .flatMap(
            e -> {
              if (!userId.equals(e.getUserId())) {
                return Mono.error(new ForbiddenException("Bu taklif sizga tegishli emas"));
              }
              e.setStatus(status);
              e.setUpdatedBy(userId);
              return staffRepository.save(e);
            })
        .flatMap(this::enrichOne);
  }

  private Mono<StaffEntity> saveStaff(StaffRequest req, UUID actorId, StaffStatus status) {
    StaffEntity e = new StaffEntity();
    e.setType(req.type());
    e.setStatus(status);
    e.setCompanyId(req.companyId());
    e.setUserId(req.userId());
    e.setPermissions(toArray(req.permissions()));
    e.setCreatedBy(actorId);
    e.setUpdatedBy(actorId);
    return staffRepository.save(e);
  }

  // ===== enrichment & helpers =====

  private Flux<StaffResponse> enrich(List<StaffEntity> staff) {
    if (staff.isEmpty()) return Flux.empty();
    Set<UUID> companyIds =
        staff.stream()
            .map(StaffEntity::getCompanyId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    Set<UUID> userIds =
        staff.stream().map(StaffEntity::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());

    Mono<Map<UUID, String>> companiesMono =
        userRepository.findAllById(companyIds).collectMap(UserEntity::getId, StaffService::companyName);
    Mono<Map<UUID, String>> usersMono =
        userRepository.findAllById(userIds).collectMap(UserEntity::getId, StaffService::fullName);

    return Mono.zip(companiesMono, usersMono)
        .flatMapMany(
            t ->
                Flux.fromIterable(
                    staff.stream()
                        .map(
                            s ->
                                toResponse(
                                    s, t.getT1().get(s.getCompanyId()), t.getT2().get(s.getUserId())))
                        .toList()));
  }

  private Mono<StaffResponse> enrichOne(StaffEntity s) {
    Mono<String> company =
        s.getCompanyId() == null
            ? Mono.just("")
            : userRepository
                .findById(s.getCompanyId())
                .map(StaffService::companyName)
                .defaultIfEmpty("");
    Mono<String> user =
        s.getUserId() == null
            ? Mono.just("")
            : userRepository.findById(s.getUserId()).map(StaffService::fullName).defaultIfEmpty("");
    return Mono.zip(company, user).map(t -> toResponse(s, t.getT1(), t.getT2()));
  }

  // COMPANY user (yuridik shaxs) nomi — legalName.
  private static String companyName(UserEntity c) {
    return c.getLegalName() != null && !c.getLegalName().isBlank() ? c.getLegalName() : "";
  }

  private static String fullName(UserEntity u) {
    return ((u.getFirstName() == null ? "" : u.getFirstName())
            + " "
            + (u.getLastName() == null ? "" : u.getLastName()))
        .trim();
  }

  private static Permission[] toArray(List<Permission> perms) {
    return perms == null ? new Permission[0] : perms.toArray(new Permission[0]);
  }

  private static List<Permission> toList(Permission[] perms) {
    return perms == null ? List.of() : List.of(perms);
  }

  private StaffResponse toResponse(StaffEntity s, String companyName, String userFullName) {
    return new StaffResponse(
        s.getId(),
        s.getType(),
        s.getStatus(),
        s.getCompanyId(),
        companyName,
        s.getUserId(),
        userFullName,
        toList(s.getPermissions()),
        s.getCreatedDate(),
        s.getCreatedBy(),
        s.getLastModifiedDate(),
        s.getUpdatedBy());
  }
}
