package uz.hesap.service.main.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.message.UserRegisteredEvent;
import uz.hesap.service.jms.JmsPublisher;
import uz.hesap.service.main.domain.UserEntity;

// Yangi foydalanuvchi ro'yxatdan o'tganda UserRegisteredEvent chiqaradi (RabbitMQ) —
// integration servis amoCRM'ga kontakt yaratadi (eski crm-app addToCrm o'rniga).
// Fire-and-forget: publish xatosi registratsiya oqimini to'xtatmaydi (onErrorResume).
@Log4j2
@Service
@RequiredArgsConstructor
public class UserRegisteredNotifier {

  private final JmsPublisher jmsPublisher;

  public Mono<Void> notifyRegistered(final UserEntity user) {
    if (user == null || user.getId() == null) {
      return Mono.empty();
    }
    final UserRegisteredEvent event =
        new UserRegisteredEvent(
            user.getId(),
            user.getFirstName(),
            user.getLastName(),
            user.getPhone(),
            user.getIn(),
            user.getType() != null ? user.getType().name() : null,
            user.getCreatedDate());
    return jmsPublisher
        .publish(event)
        .then()
        .onErrorResume(
            e -> {
              log.warn("UserRegisteredEvent publish failed: {}", e.getMessage());
              return Mono.empty();
            });
  }
}
