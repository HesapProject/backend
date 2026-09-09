package uz.hesap.service.log.jms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Delivery;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.message.PaymeLogReply;
import uz.hesap.service.jms.BaseConsumer;
import uz.hesap.service.jms.JmsConsumer;
import uz.hesap.service.log.service.PaymeLogService;

@Component
@RequiredArgsConstructor
@JmsConsumer(targetType = "PaymeLogReply")
public class PaymeLogConsumer extends BaseConsumer {
  private static final Logger LOGGER = LogManager.getLogger();
  private final ObjectMapper objectMapper;
  private final PaymeLogService paymeLogService;

  @Override
  protected Mono<Void> consume(final Delivery message) {
    LOGGER.debug("Processing PaymeLogReply message: {}", message);

    PaymeLogReply model = convertMessage(message);
    return paymeLogService.setLog(model);
  }

  private PaymeLogReply convertMessage(Delivery message) {
    try {
      String json = new String(message.getBody(), StandardCharsets.UTF_8);
      return objectMapper.readValue(json, PaymeLogReply.class);
    } catch (Exception e) {
      LOGGER.error("Failed to parse PaymeLogReply: {}", e.getMessage());
      throw new RuntimeException("Error parsing PaymeLogReply", e);
    }
  }
}
