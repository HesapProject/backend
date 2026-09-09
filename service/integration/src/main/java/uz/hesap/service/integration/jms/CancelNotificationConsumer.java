package uz.hesap.service.integration.jms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Delivery;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.message.CancelNotificationReply;
import uz.hesap.service.jms.BaseConsumer;
import uz.hesap.service.jms.JmsConsumer;
import uz.hesap.service.integration.service.NotificationService;

@Component
@RequiredArgsConstructor
@JmsConsumer(targetType = "CancelNotificationReply")
public class CancelNotificationConsumer extends BaseConsumer {
  private static final Logger LOGGER = LogManager.getLogger();
  private final ObjectMapper objectMapper;
  private final NotificationService notificationService;

  @Override
  protected Mono<Void> consume(final Delivery message) {
    LOGGER.debug("Processing CancelNotificationReply message: {}", message);
    try {
      String json = new String(message.getBody(), StandardCharsets.UTF_8);
      CancelNotificationReply reply = objectMapper.readValue(json, CancelNotificationReply.class);
      return notificationService.deleteNotificationLogic(reply.dataId());
    } catch (Exception e) {
      LOGGER.error("Failed to parse CancelNotificationReply: {}", e.getMessage());
      return Mono.error(new RuntimeException("Error parsing CancelNotificationReply", e));
    }
  }
}
