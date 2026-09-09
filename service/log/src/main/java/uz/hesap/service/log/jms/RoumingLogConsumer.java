package uz.hesap.service.log.jms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Delivery;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.message.RoumingLogReply;
import uz.hesap.service.jms.BaseConsumer;
import uz.hesap.service.jms.JmsConsumer;
import uz.hesap.service.log.service.RoumingLogService;

@Component
@RequiredArgsConstructor
@JmsConsumer(targetType = "RoumingLogReply")
public class RoumingLogConsumer extends BaseConsumer {
  private static final Logger LOGGER = LogManager.getLogger();
  private final ObjectMapper objectMapper;
  private final RoumingLogService roumingLogService;

  @Override
  protected Mono<Void> consume(final Delivery message) {
    LOGGER.debug("Processing RoumingLogReply message: {}", message);

    RoumingLogReply model = convertMessage(message);
    return roumingLogService.setLog(model);
  }

  private RoumingLogReply convertMessage(Delivery message) {
    try {
      String json = new String(message.getBody(), StandardCharsets.UTF_8);
      return objectMapper.readValue(json, RoumingLogReply.class);
    } catch (Exception e) {
      LOGGER.error("Failed to parse RoumingLogReply: {}", e.getMessage());
      throw new RuntimeException("Error parsing RoumingLogReply", e);
    }
  }
}
