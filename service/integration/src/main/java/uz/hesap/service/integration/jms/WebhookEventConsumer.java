package uz.hesap.service.integration.jms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Delivery;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.message.WebhookEvent;
import uz.hesap.service.integration.service.WebhookSender;
import uz.hesap.service.integration.webclient.ApiKeyServiceClient;
import uz.hesap.service.jms.BaseConsumer;
import uz.hesap.service.jms.JmsConsumer;

/**
 * Document servisdan kelgan {@link WebhookEvent}'ni shartnoma taraflarining OpenAPI kalitlariga
 * biriktirilgan `webhookUrl`larga yetkazadi. Kalit hodisaga obuna bo'lmagan bo'lsa
 * (webhookEvents ro'yxati) o'tkazib yuboriladi.
 */
@Component
@RequiredArgsConstructor
@JmsConsumer(targetType = "WebhookEvent")
public class WebhookEventConsumer extends BaseConsumer {

  private static final Logger LOGGER = LogManager.getLogger();

  private final ObjectMapper objectMapper;
  private final ApiKeyServiceClient apiKeyServiceClient;
  private final WebhookSender webhookSender;

  @Override
  protected Mono<Void> consume(final Delivery message) {
    WebhookEvent event = convert(message);
    if (event == null || event.event() == null) {
      return Mono.empty();
    }
    Set<String> parties = parties(event);
    if (parties.isEmpty()) {
      return Mono.empty();
    }
    return apiKeyServiceClient
        .webhookTargets(parties)
        .filter(target -> target.subscribedTo(event.event()))
        .flatMap(target -> webhookSender.send(target, event))
        .then()
        .onErrorResume(
            e -> {
              LOGGER.error("WebhookEvent [{}] ishlanmadi: {}", event.event(), e.getMessage());
              return Mono.empty();
            });
  }

  // Hodisa qaysi identifikatorlarga tegishli: xaridor, sotuvchi, yaratuvchi.
  private Set<String> parties(final WebhookEvent event) {
    Set<String> ins = new LinkedHashSet<>();
    addIfPresent(ins, event.buyerIn());
    addIfPresent(ins, event.sellerIn());
    addIfPresent(ins, event.creatorIn());
    return ins;
  }

  private void addIfPresent(final Set<String> target, final String value) {
    if (value != null && !value.isBlank()) {
      target.add(value);
    }
  }

  private WebhookEvent convert(final Delivery message) {
    try {
      String json = new String(message.getBody(), StandardCharsets.UTF_8);
      return objectMapper.readValue(json, WebhookEvent.class);
    } catch (Exception e) {
      LOGGER.error("WebhookEvent parse qilinmadi: {}", e.getMessage());
      return null;
    }
  }
}
