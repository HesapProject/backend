package uz.hesap.service.integration.service;

import static uz.hesap.service.common.exception.handler.ErrorCode.NOT_FOUND_ERROR_CODE;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.exception.NotFoundException;
import uz.hesap.service.common.util.UserPrincipal;
import uz.hesap.service.common.util.message.FirebaseNotificationReply;
import uz.hesap.service.integration.domain.NotificationEntity;
import uz.hesap.service.integration.model.NotificationResponse;
import uz.hesap.service.integration.model.mapper.NotificationMapper;
import uz.hesap.service.integration.repository.NotificationRepository;

@Log4j2
@Service
@RequiredArgsConstructor
public class NotificationService {

  private final NotificationRepository notificationRepository;
  private final FirebaseProvider firebaseProvider;

  public Mono<Page<NotificationResponse>> getAllForUser(
      UserPrincipal userPrincipal, Pageable pageable) {
    UUID userId = userPrincipal.user().id();
    return notificationRepository
        .findByUserIdAndDeletedIsFalse(userId, pageable)
        .map(NotificationMapper.INSTANCE::toResponse)
        .collectList()
        .zipWith(notificationRepository.countByUserIdAndDeletedIsFalse(userId))
        .map(tuple -> new PageImpl<>(tuple.getT1(), pageable, tuple.getT2()));
  }

  public Mono<NotificationResponse> getById(UserPrincipal userPrincipal, UUID id) {
    return notificationRepository
        .findByIdAndUserIdAndDeletedIsFalse(id, userPrincipal.user().id())
        .switchIfEmpty(
            Mono.error(new NotFoundException(NOT_FOUND_ERROR_CODE, "Notification not found")))
        .flatMap(
            notification -> {
              notification.setIsViewed(Boolean.TRUE);
              return notificationRepository.save(notification);
            })
        .map(NotificationMapper.INSTANCE::toResponse);
  }

  // Foydalanuvchining barcha bildirishnomalarini o'qilgan (isViewed=true) qiladi.
  public Mono<Void> markAllViewed(UserPrincipal userPrincipal) {
    return notificationRepository.markAllViewedByUserId(userPrincipal.user().id());
  }

  public Mono<Void> saveNotification(NotificationEntity entity) {
    return notificationRepository.save(entity).then();
  }

  public Mono<Void> deleteNotificationLogic(UUID dataId) {
    return notificationRepository.deleteNotificationByDataId(dataId);
  }

  // In-app bildirishnomani DB'ga saqlaydi + FCM yuborishni integration servisga topshiradi.
  public Mono<Void> send(FirebaseNotificationReply model) {
    NotificationEntity entity = new NotificationEntity();
    entity.setDataId(model.dataId());
    entity.setType(model.type());
    entity.setUserId(model.toUserId());

    if (model.title() != null) {
      entity.setTitleUz(model.title().uz());
      entity.setTitleRu(model.title().ru());
      entity.setTitleEn(model.title().en());
    }
    if (model.body() != null) {
      entity.setBodyUz(model.body().uz());
      entity.setBodyRu(model.body().ru());
      entity.setBodyEn(model.body().en());
    }

    // In-app saqlangach FCM to'g'ridan-to'g'ri shu servisdagi FirebaseProvider orqali.
    return saveNotification(entity).then(firebaseProvider.sendUser(model));
  }
}
