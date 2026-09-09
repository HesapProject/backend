package uz.hesap.service.main.service;

import static uz.hesap.service.common.exception.handler.ErrorCode.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.exception.BadRequestException;
import uz.hesap.service.common.exception.NotFoundException;
import uz.hesap.service.common.util.UserBasicResponse;
import uz.hesap.service.common.util.UserResponse;
import uz.hesap.service.common.util.enums.UserType;
import uz.hesap.service.common.util.message.CancelNotificationReply;
import uz.hesap.service.common.util.message.FirebaseNotificationReply;
import uz.hesap.service.jms.JmsPublisher;
import uz.hesap.service.main.domain.PermissionRequestEntity;
import uz.hesap.service.main.domain.PermissionRequestStatus;
import uz.hesap.service.main.domain.UserEntity;
import uz.hesap.service.main.domain.UserPermissionEntity;
import uz.hesap.service.main.model.mapper.PermissionRequestMapper;
import uz.hesap.service.main.model.mapper.UserMapper;
import uz.hesap.service.main.model.request.PermissionActionRequest;
import uz.hesap.service.main.model.response.PermissionRequestResponse;
import uz.hesap.service.main.model.response.UserPermissionResponse;
import uz.hesap.service.main.repository.PermissionRequestRepository;
import uz.hesap.service.main.repository.UserPermissionRepository;
import uz.hesap.service.main.repository.UserRepository;

@Log4j2
@Service
@RequiredArgsConstructor
public class PermissionRequestService {

  private final PermissionRequestRepository permissionRequestRepository;
  private final UserPermissionRepository userPermissionRepository;
  private final UserRepository userRepository;
  private final JmsPublisher jmsPublisher;
  private final SessionService sessionService;

  public Mono<Page<PermissionRequestResponse>> getIncomingRequests(
      UUID currentUserId, Pageable pageable) {
    log.debug("Get incoming permission requests for userId [{}]", currentUserId);
    return permissionRequestRepository
        .findByUserToIdAndDeletedIsFalseAndStatusNot(
            currentUserId, PermissionRequestStatus.CANCELLED, pageable)
        .collectList()
        .flatMap(this::enrichPermissionRequests)
        .zipWith(
            permissionRequestRepository.countByUserToIdAndDeletedIsFalseAndStatusNot(
                currentUserId, PermissionRequestStatus.CANCELLED))
        .map(tuple -> new PageImpl<>(tuple.getT1(), pageable, tuple.getT2()));
  }

  public Mono<Page<PermissionRequestResponse>> getOutgoingRequests(
      UUID currentUserId, Pageable pageable) {
    log.debug("Get outgoing permission requests for userId [{}]", currentUserId);
    return permissionRequestRepository
        .findByUserFromIdAndDeletedIsFalseAndStatusNot(
            currentUserId, PermissionRequestStatus.CANCELLED, pageable)
        .collectList()
        .flatMap(this::enrichPermissionRequests)
        .zipWith(
            permissionRequestRepository.countByUserFromIdAndDeletedIsFalseAndStatusNot(
                currentUserId, PermissionRequestStatus.CANCELLED))
        .map(tuple -> new PageImpl<>(tuple.getT1(), pageable, tuple.getT2()));
  }

  public Mono<PermissionRequestResponse> findToUserRequest(UUID fromUserId, UUID toUserId) {
    log.debug("Get permission request by from  [{}] to [{}]", fromUserId, toUserId);
    return permissionRequestRepository
        .findFirstByUserFromIdAndUserToIdAndDeletedIsFalseOrderByCreatedDateDesc(
            fromUserId, toUserId)
        .flatMap(this::enrichPermissionRequest);
  }

  public Mono<PermissionRequestResponse> getRequestById(UUID requestId) {
    log.debug("Get permission request by id [{}]", requestId);
    return permissionRequestRepository
        .findByIdAndDeletedIsFalse(requestId)
        .switchIfEmpty(
            Mono.error(new NotFoundException(NOT_FOUND_ERROR_CODE, "Permission request not found")))
        .flatMap(this::enrichPermissionRequest);
  }

  // ======================== CREATE REQUEST ========================
  @Transactional
  public Mono<PermissionRequestResponse> createRequest(
      UserResponse fromUser, UUID toUserID, PermissionActionRequest request) {

    log.debug("Create permission request from userId [{}] to userId [{}]", toUserID, fromUser.id());

    return validateInitialRequest(fromUser.id(), toUserID, request)
        .then(checkTargetUserExists(toUserID))
        .flatMap(
            toUser ->
                Mono.zip(
                        fetchExistingPermission(toUser.getIn(), fromUser.in()),
                        fetchPendingRequest(toUserID, fromUser.id()))
                    .flatMap(
                        tuple ->
                            prepareNewRequestEntity(
                                fromUser.id(),
                                toUser.getId(),
                                request,
                                tuple.getT1(),
                                tuple.getT2()))
                    .flatMap(permissionRequestRepository::save)
                    .flatMap(this::enrichPermissionRequest)
                    .delayUntil(
                        response ->
                            sessionService
                                .findUserFirebaseTokens(toUserID)
                                .collectList()
                                .flatMap(
                                    tokens ->
                                        jmsPublisher.publish(
                                            FirebaseNotificationReply.permission(
                                                response.id(),
                                                UserMapper.INSTANCE.toUserResponse(toUser),
                                                fromUser,
                                                tokens)))));
  }

  // ======================== ACCEPT ========================

  @Transactional
  public Mono<Void> acceptRequest(
      UserResponse toUser, UUID fromUserId, PermissionActionRequest request) {
    log.debug("Accept permission request from userId [{}] by userId [{}]", fromUserId, toUser.id());
    UUID toUserId = toUser.id();

    return permissionRequestRepository
        .findByUserFromIdAndUserToIdAndStatusAndDeletedIsFalse(
            fromUserId, toUserId, PermissionRequestStatus.PENDING)
        .switchIfEmpty(
            Mono.error(
                new NotFoundException(
                    NOT_FOUND_ERROR_CODE, "Pending permission request not found")))
        .flatMap(
            permRequest -> {
              // Update request status
              permRequest.setStatus(PermissionRequestStatus.ACCEPTED);
              if (request != null) {
                permRequest.setPassport(
                    Boolean.TRUE.equals(request.passport())
                        && Boolean.TRUE.equals(permRequest.getPassport()));
                permRequest.setPayability(
                    Boolean.TRUE.equals(request.payability())
                        && Boolean.TRUE.equals(permRequest.getPayability()));
                permRequest.setContract(
                    Boolean.TRUE.equals(request.contract())
                        && Boolean.TRUE.equals(permRequest.getContract()));
                permRequest.setPartner(
                    Boolean.TRUE.equals(request.partner())
                        && Boolean.TRUE.equals(permRequest.getPartner()));
              }

              return permissionRequestRepository
                  .save(permRequest)
                  .flatMap(this::addOrUpdateUserPermission)
                  .delayUntil(
                      permission ->
                          sessionService
                              .findUserFirebaseTokens(fromUserId)
                              .collectList()
                              .flatMap(
                                  tokens ->
                                      jmsPublisher.publish(
                                          FirebaseNotificationReply.permissionAccepted(
                                              permRequest.getId(), toUser, fromUserId, tokens))));
            })
        .then();
  }

  // ======================== REJECT ========================
  public Mono<Void> rejectRequest(UserResponse toUser, UUID fromUserId) {
    UUID toUserId = toUser.id();
    log.debug("Reject permission request from userId [{}] to userId [{}]", fromUserId, toUserId);

    return permissionRequestRepository
        .findFirstByUserFromIdAndUserToIdAndStatusAndDeletedIsFalseOrderByCreatedDateDesc(
            fromUserId, toUserId, PermissionRequestStatus.PENDING)
        .switchIfEmpty(
            Mono.error(
                new NotFoundException(
                    NOT_FOUND_ERROR_CODE, "Pending permission request not found")))
        .flatMap(
            permRequest -> {
              permRequest.setStatus(PermissionRequestStatus.REJECTED);
              return permissionRequestRepository
                  .save(permRequest)
                  .delayUntil(
                      saved ->
                          sessionService
                              .findUserFirebaseTokens(fromUserId)
                              .collectList()
                              .flatMap(
                                  tokens ->
                                      jmsPublisher.publish(
                                          FirebaseNotificationReply.permissionRejected(
                                              saved.getId(), toUser, fromUserId, tokens))));
            })
        .then();
  }

  public Mono<Void> cancelRequest(UUID fromUserId, UUID toUserID) {
    log.debug("Cancel permission request by userId [{}] to userId [{}]", fromUserId, toUserID);

    return permissionRequestRepository
        .findFirstByUserFromIdAndUserToIdAndStatusAndDeletedIsFalseOrderByCreatedDateDesc(
            fromUserId, toUserID, PermissionRequestStatus.PENDING)
        .switchIfEmpty(
            Mono.error(
                new NotFoundException(
                    NOT_FOUND_ERROR_CODE, "Pending permission request not found")))
        .flatMap(
            permRequest -> {
              permRequest.setStatus(PermissionRequestStatus.CANCELLED);
              return permissionRequestRepository
                  .save(permRequest)
                  .delayUntil(
                      saved -> jmsPublisher.publish(new CancelNotificationReply(saved.getId())));
            })
        .then();
  }

  // ======================== GET ACTIVE USER PERMISSIONS ========================
  public Mono<Page<UserPermissionResponse>> getGrantedPermissions(
      String currentUserIn, Pageable pageable) {
    log.debug("Get granted user permissions for IN [{}]", currentUserIn);
    return userPermissionRepository
        .findByUserFromInAndDeletedIsFalseAndActiveIsTrue(currentUserIn)
        .collectList()
        .flatMap(this::enrichUserPermissions)
        .map(list -> new PageImpl<>(list, pageable, list.size()));
  }

  // ======================== GET ACTIVE USER PERMISSIONS ========================
  public Mono<UserPermissionResponse> findToUserActivePermission(
      String currentUserIn, String toUserIn) {
    log.debug("Get granted user permissions from IN [{}] to IN [{}] ", currentUserIn, toUserIn);
    return userPermissionRepository
        .findByUserFromInAndUserToInAndDeletedIsFalse(toUserIn, currentUserIn)
        .map(PermissionRequestMapper.INSTANCE::toPermissionResponse);
  }

  public Mono<Page<UserPermissionResponse>> getReceivedPermissions(
      String currentUserIn, Pageable pageable) {
    log.debug("Get received user permissions for IN [{}]", currentUserIn);
    return userPermissionRepository
        .findByUserToInAndDeletedIsFalseAndActiveIsTrue(currentUserIn)
        .collectList()
        .flatMap(this::enrichUserPermissions)
        .map(list -> new PageImpl<>(list, pageable, list.size()));
  }

  public Mono<Long> expireAllPermissions() {
    log.debug("Expire all user permissions older than 24 hours");
    return userPermissionRepository.expireAllPermissions();
  }

  private Mono<List<PermissionRequestResponse>> enrichPermissionRequests(
      List<PermissionRequestEntity> entities) {
    if (entities == null || entities.isEmpty()) return Mono.just(List.of());

    Set<UUID> userIds = new HashSet<>();
    entities.forEach(
        entity -> {
          if (entity.getUserFromId() != null) userIds.add(entity.getUserFromId());
          if (entity.getUserToId() != null) userIds.add(entity.getUserToId());
        });

    return userRepository
        .findAllByIdIn(new ArrayList<>(userIds))
        .map(PermissionRequestMapper.INSTANCE::toUserBasicResponse)
        .collectMap(UserBasicResponse::id)
        .map(
            userMap ->
                entities.stream()
                    .map(
                        entity -> {
                          UserBasicResponse userFrom =
                              userMap.getOrDefault(
                                  entity.getUserFromId(),
                                  new UserBasicResponse(entity.getUserFromId(), null, null, null, null, null));
                          UserBasicResponse userTo =
                              userMap.getOrDefault(
                                  entity.getUserToId(),
                                  new UserBasicResponse(entity.getUserToId(), null, null, null, null, null));
                          return PermissionRequestMapper.INSTANCE.toResponse(
                              entity, userFrom, userTo);
                        })
                    .toList());
  }

  private Mono<List<UserPermissionResponse>> enrichUserPermissions(
      List<UserPermissionEntity> entities) {
    if (entities == null || entities.isEmpty()) return Mono.just(List.of());

    // white_list IN-asosli — userlarni IN (PINFL) bo'yicha to'playmiz.
    Set<String> ins = new HashSet<>();
    entities.forEach(
        entity -> {
          if (entity.getUserFromIn() != null) ins.add(entity.getUserFromIn());
          if (entity.getUserToIn() != null) ins.add(entity.getUserToIn());
        });

    return userRepository
        .findAllByInIn(new ArrayList<>(ins))
        .collectMap(UserEntity::getIn, PermissionRequestMapper.INSTANCE::toUserBasicResponse)
        .map(
            userMap ->
                entities.stream()
                    .map(
                        entity -> {
                          UserBasicResponse userFrom =
                              userMap.getOrDefault(
                                  entity.getUserFromIn(),
                                  new UserBasicResponse(null, null, null, null, null, null));
                          UserBasicResponse userTo =
                              userMap.getOrDefault(
                                  entity.getUserToIn(),
                                  new UserBasicResponse(null, null, null, null, null, null));
                          return PermissionRequestMapper.INSTANCE.toUserPermissionResponse(
                              entity, userFrom, userTo);
                        })
                    .toList());
  }

  private Mono<PermissionRequestResponse> enrichPermissionRequest(PermissionRequestEntity entity) {
    if (entity == null) return null;
    Mono<UserBasicResponse> userFrom =
        userRepository
            .findByIdAndDeletedIsFalse(entity.getUserFromId())
            .map(PermissionRequestMapper.INSTANCE::toUserBasicResponse)
            .defaultIfEmpty(new UserBasicResponse(entity.getUserFromId(), null, null, null, null, null));

    Mono<UserBasicResponse> userTo =
        userRepository
            .findByIdAndDeletedIsFalse(entity.getUserToId())
            .map(PermissionRequestMapper.INSTANCE::toUserBasicResponse)
            .defaultIfEmpty(new UserBasicResponse(entity.getUserToId(), null, null, null, null, null));

    return Mono.zip(userFrom, userTo)
        .map(
            tuple ->
                PermissionRequestMapper.INSTANCE.toResponse(entity, tuple.getT1(), tuple.getT2()));
  }

  private Mono<UserPermissionEntity> addOrUpdateUserPermission(PermissionRequestEntity request) {
    // white_list IN-asosli: so'rovdagi UUID'larni IN (PINFL)'ga aylantiramiz.
    // white_list.userFrom = so'rovning TO useri (ma'lumot egasi),
    // white_list.userTo   = so'rovning FROM useri (so'rovchi).
    return Mono.zip(
            userRepository.findByIdAndDeletedIsFalse(request.getUserToId()).map(UserEntity::getIn),
            userRepository
                .findByIdAndDeletedIsFalse(request.getUserFromId())
                .map(UserEntity::getIn))
        .flatMap(
            ins -> {
              String fromIn = ins.getT1();
              String toIn = ins.getT2();
              return userPermissionRepository
                  .findByUserFromInAndUserToInAndDeletedIsFalse(fromIn, toIn)
                  .flatMap(
                      existing -> {
                        if (Boolean.TRUE.equals(request.getUserInfo()))
                          existing.setUserInfo(Boolean.TRUE);
                        if (Boolean.TRUE.equals(request.getPassport()))
                          existing.setPassport(Boolean.TRUE);
                        if (Boolean.TRUE.equals(request.getPayability()))
                          existing.setPayability(Boolean.TRUE);
                        if (Boolean.TRUE.equals(request.getContract()))
                          existing.setContract(Boolean.TRUE);
                        if (Boolean.TRUE.equals(request.getPartner()))
                          existing.setPartner(Boolean.TRUE);
                        existing.setActive(Boolean.TRUE);
                        return userPermissionRepository.save(existing);
                      })
                  .switchIfEmpty(
                      Mono.defer(
                          () -> {
                            UserPermissionEntity entity = new UserPermissionEntity();
                            entity.setUserFromIn(fromIn);
                            entity.setUserToIn(toIn);
                            entity.setUserInfo(Boolean.TRUE.equals(request.getUserInfo()));
                            entity.setPassport(Boolean.TRUE.equals(request.getPassport()));
                            entity.setPayability(Boolean.TRUE.equals(request.getPayability()));
                            entity.setContract(Boolean.TRUE.equals(request.getContract()));
                            entity.setPartner(Boolean.TRUE.equals(request.getPartner()));
                            entity.setActive(Boolean.TRUE);
                            return userPermissionRepository.save(entity);
                          }));
            });
  }

  private Mono<Void> validateInitialRequest(
      UUID currentUserId, UUID targetUserId, PermissionActionRequest request) {
    if (currentUserId.equals(targetUserId)) {
      return Mono.error(
          new BadRequestException(
              BAD_REQUEST_CODE, "Cannot create permission request to yourself"));
    }
    if (!Boolean.TRUE.equals(request.userInfo())
        && !Boolean.TRUE.equals(request.passport())
        && !Boolean.TRUE.equals(request.payability())
        && !Boolean.TRUE.equals(request.contract())
        && !Boolean.TRUE.equals(request.partner())) {
      throw new BadRequestException(BAD_REQUEST_CODE, "At least one permission must be true");
    }
    return Mono.empty();
  }

  private Mono<UserEntity> checkTargetUserExists(UUID targetUserId) {
    return userRepository
        .findByIdAndTypeAndDeletedIsFalse(targetUserId, UserType.CLIENT)
        .switchIfEmpty(Mono.error(new NotFoundException(USER_NOT_FOUND, "Target user not found")));
  }

  private Mono<UserPermissionEntity> fetchExistingPermission(String toUserIn, String fromUserIn) {
    return userPermissionRepository
        .findByUserFromInAndUserToInAndDeletedIsFalse(fromUserIn, toUserIn)
        .defaultIfEmpty(new UserPermissionEntity());
  }

  private Mono<PermissionRequestEntity> fetchPendingRequest(UUID toUserId, UUID fromUserId) {
    return permissionRequestRepository
        .findFirstByUserFromIdAndUserToIdAndStatusAndDeletedIsFalseOrderByCreatedDateDesc(
            fromUserId, toUserId, PermissionRequestStatus.PENDING)
        .defaultIfEmpty(new PermissionRequestEntity());
  }

  private Mono<PermissionRequestEntity> prepareNewRequestEntity(
      UUID fromUserId,
      UUID toUserID,
      PermissionActionRequest request,
      UserPermissionEntity existing,
      PermissionRequestEntity pending) {
    // todo delete all permission
    boolean userInfo =
        isPermissionRequired(existing.getUserInfo(), pending.getUserInfo(), request.userInfo());
    boolean passport =
        isPermissionRequired(existing.getPassport(), pending.getPassport(), request.passport());
    boolean payability =
        isPermissionRequired(
            existing.getPayability(), pending.getPayability(), request.payability());
    boolean contract =
        isPermissionRequired(existing.getContract(), pending.getContract(), request.contract());
    boolean partner =
        isPermissionRequired(existing.getPartner(), pending.getPartner(), request.partner());

    if (!(passport || payability || contract || partner || userInfo)) {
      return Mono.error(
          new BadRequestException(
              ALREADY_EXISTS_ERROR_CODE,
              "All requested permissions are already granted or pending"));
    }

    PermissionRequestEntity entity =
        pending.getId() != null ? pending : new PermissionRequestEntity();
    entity.setUserFromId(fromUserId);
    entity.setUserToId(toUserID);
    entity.setPassport(passport);
    entity.setUserInfo(userInfo);
    entity.setPayability(payability);
    entity.setContract(contract);
    entity.setPartner(partner);
    entity.setStatus(PermissionRequestStatus.PENDING);
    return Mono.just(entity);
  }

  private boolean isPermissionRequired(Boolean existing, Boolean pending, Boolean requested) {
    return !Boolean.TRUE.equals(existing)
        && (Boolean.TRUE.equals(pending) || Boolean.TRUE.equals(requested));
  }
}
