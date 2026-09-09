package uz.hesap.service.integration.jms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Delivery;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.message.SmsReply;
import uz.hesap.service.integration.service.EskizProvider;
import uz.hesap.service.jms.BaseConsumer;
import uz.hesap.service.jms.JmsConsumer;

// SMS yuborish so'rovini (SmsReply) RabbitMQ'dan qabul qilib Eskiz orqali yuboradi.
@Component
@RequiredArgsConstructor
@JmsConsumer(targetType = "SmsReply")
public class SendSmsConsumer extends BaseConsumer {
  private static final Logger LOGGER = LogManager.getLogger();
  private final ObjectMapper objectMapper;
  private final EskizProvider eskizProvider;

  @Override
  protected Mono<Void> consume(final Delivery message) {
    SmsReply model = convertMessage(message);
    LOGGER.debug("Processing SmsReply message for phone: {}", model.phone());
    return eskizProvider.send(model.phone(), model.message()).then();
  }

  private SmsReply convertMessage(Delivery message) {
    try {
      String json = new String(message.getBody(), StandardCharsets.UTF_8);
      return objectMapper.readValue(json, SmsReply.class);
    } catch (Exception e) {
      LOGGER.error("Failed to parse SmsReply: {}", e.getMessage());
      throw new RuntimeException("Error parsing SmsReply", e);
    }
  }
}
