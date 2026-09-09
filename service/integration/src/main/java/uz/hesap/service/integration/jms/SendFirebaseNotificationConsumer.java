package uz.hesap.service.integration.jms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Delivery;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.message.FirebaseNotificationReply;
import uz.hesap.service.jms.BaseConsumer;
import uz.hesap.service.jms.JmsConsumer;
import uz.hesap.service.integration.service.NotificationService;

@Component
@RequiredArgsConstructor
@JmsConsumer(targetType = "FirebaseNotificationReply")
public class SendFirebaseNotificationConsumer extends BaseConsumer {
  private static final Logger LOGGER = LogManager.getLogger();
  private final ObjectMapper objectMapper;
  private final NotificationService notificationService;

  @Override
  protected Mono<Void> consume(final Delivery message) {

    FirebaseNotificationReply model = convertMessage(message);
    LOGGER.debug("Processing FirebaseNotificationReply message: {}", model);
    return notificationService.send(model);
  }

  private FirebaseNotificationReply convertMessage(Delivery message) {
    try {
      String json = new String(message.getBody(), StandardCharsets.UTF_8);
      return objectMapper.readValue(json, FirebaseNotificationReply.class);
    } catch (Exception e) {
      LOGGER.error("Failed to parse UserLogReason: {}", e.getMessage());
      throw new RuntimeException("Error parsing UserLogReason", e);
    }
  }
}
