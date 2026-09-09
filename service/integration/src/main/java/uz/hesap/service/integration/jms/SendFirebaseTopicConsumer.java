package uz.hesap.service.integration.jms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Delivery;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.message.FirebaseTopicReply;
import uz.hesap.service.integration.service.FirebaseProvider;
import uz.hesap.service.jms.BaseConsumer;
import uz.hesap.service.jms.JmsConsumer;

// Topic (BLOG/yangilik) FCM push so'rovini (FirebaseTopicReply) RabbitMQ'dan qabul qilib yuboradi.
@Component
@RequiredArgsConstructor
@JmsConsumer(targetType = "FirebaseTopicReply")
public class SendFirebaseTopicConsumer extends BaseConsumer {
  private static final Logger LOGGER = LogManager.getLogger();
  private final ObjectMapper objectMapper;
  private final FirebaseProvider firebaseProvider;

  @Override
  protected Mono<Void> consume(final Delivery message) {
    FirebaseTopicReply model = convertMessage(message);
    LOGGER.debug("Processing FirebaseTopicReply message: topic={}", model.topic());
    return firebaseProvider.sendTopic(model);
  }

  private FirebaseTopicReply convertMessage(Delivery message) {
    try {
      String json = new String(message.getBody(), StandardCharsets.UTF_8);
      return objectMapper.readValue(json, FirebaseTopicReply.class);
    } catch (Exception e) {
      LOGGER.error("Failed to parse FirebaseTopicReply: {}", e.getMessage());
      throw new RuntimeException("Error parsing FirebaseTopicReply", e);
    }
  }
}
