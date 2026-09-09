package uz.hesap.service.document.service.document;

import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.enums.WebhookEventType;
import uz.hesap.service.common.util.message.WebhookEvent;
import uz.hesap.service.document.domain.document.DocumentEntity;
import uz.hesap.service.jms.JmsPublisher;

/**
 * OpenAPI webhook hodisalarini RabbitMQ'ga yuboradi — integration servis ularni hamkorning
 * `webhookUrl`iga yetkazadi. Best-effort: xatolik asosiy oqimni to'xtatmaydi
 * ({@link ContractTimelinePublisher} bilan bir xil yondashuv).
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class WebhookEventPublisher {

  private final JmsPublisher jmsPublisher;

  public void publish(final WebhookEventType type, final DocumentEntity doc, final String actorIn) {
    if (doc == null) {
      return;
    }
    WebhookEvent event =
        new WebhookEvent(
            type,
            doc.getId(),
            doc.getNumber(),
            doc.getStatus() != null ? doc.getStatus().name() : null,
            doc.getTemplateId(),
            doc.getBuyerIn(),
            doc.getSellerIn(),
            doc.getCreatorIn(),
            actorIn,
            doc.getPrice(),
            doc.getCurrency() != null ? doc.getCurrency().name() : null,
            Instant.now());
    send(event, doc.getId());
  }

  // To'lov hodisasi — hujjat entity'si qo'lda bo'lmaganda (to'lov so'rovi konteksti).
  // Taraflar so'rovning o'zidan olinadi, shunda integration kimga yuborishni biladi.
  public void publishPayment(
      final UUID contractId,
      final String buyerIn,
      final String sellerIn,
      final String actorIn,
      final Double amount,
      final String currency) {
    WebhookEvent event =
        new WebhookEvent(
            WebhookEventType.PAYMENT_ACCEPTED,
            contractId,
            null,
            null,
            null,
            buyerIn,
            sellerIn,
            null,
            actorIn,
            amount,
            currency,
            Instant.now());
    send(event, contractId);
  }

  private void send(final WebhookEvent event, final UUID contractId) {
    jmsPublisher
        .publish(event)
        .onErrorResume(
            e -> {
              log.warn("WebhookEvent publish failed for {}: {}", contractId, e.getMessage());
              return Mono.empty();
            })
        .subscribe();
  }
}
