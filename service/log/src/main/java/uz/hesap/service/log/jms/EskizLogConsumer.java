package uz.hesap.service.log.jms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Delivery;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.message.EskizLogReply;
import uz.hesap.service.jms.BaseConsumer;
import uz.hesap.service.jms.JmsConsumer;
import uz.hesap.service.log.service.EskizLogService;

@Component
@RequiredArgsConstructor
@JmsConsumer(targetType = "EskizLogReply")
public class EskizLogConsumer extends BaseConsumer {
  private static final Logger LOGGER = LogManager.getLogger();
  private final ObjectMapper objectMapper;
  private final EskizLogService eskizLogService;

  @Override
  protected Mono<Void> consume(final Delivery message) {
    LOGGER.debug("Processing EskizLogReply message: {}", message);

    EskizLogReply model = convertMessage(message);
    return eskizLogService.saveLog(model);
  }

  private EskizLogReply convertMessage(Delivery message) {
    try {
      String json = new String(message.getBody(), StandardCharsets.UTF_8);
      return objectMapper.readValue(json, EskizLogReply.class);
    } catch (Exception e) {
      LOGGER.error("Failed to parse UserLogReason: {}", e.getMessage());
      throw new RuntimeException("Error parsing UserLogReason", e);
    }
  }
}
