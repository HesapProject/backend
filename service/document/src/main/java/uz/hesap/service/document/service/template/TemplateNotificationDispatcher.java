package uz.hesap.service.document.service.template;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.message.ContractNotificationEvent;
import uz.hesap.service.common.util.message.NotificationEvent;
import uz.hesap.service.jms.JmsPublisher;

/**
 * Kontrakt hodisasini RabbitMQ'ga generic {@link ContractNotificationEvent} sifatida e'lon qiladi.
 * Document servis matn QURMAYDI va config'ni O'QIMAYDI — qaror (SMS/push yoq-o'chiq, matn) integration
 * servisida o'z template_notification config'iga qarab amalga oshiriladi. Barcha send-point'lar shu
 * komponentdan foydalanadi.
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class TemplateNotificationDispatcher {

  private final JmsPublisher jmsPublisher;

  public Mono<Void> dispatch(
      final UUID templateId,
      final NotificationEvent event,
      final UUID recipientId,
      final String docNumber,
      final String senderName,
      final UUID documentId) {
    if (templateId == null || event == null || recipientId == null) {
      return Mono.empty();
    }
    return jmsPublisher
        .publish(
            new ContractNotificationEvent(
                event, templateId, documentId, docNumber, recipientId, senderName))
        .onErrorResume(
            e -> {
              log.error("ContractNotificationEvent publish failed [{}]: {}", event, e.getMessage());
              return Mono.empty();
            });
  }
}
