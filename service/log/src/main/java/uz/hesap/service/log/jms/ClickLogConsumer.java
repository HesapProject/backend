package uz.hesap.service.log.jms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Delivery;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.message.ClickLogReply;
import uz.hesap.service.jms.BaseConsumer;
import uz.hesap.service.jms.JmsConsumer;
import uz.hesap.service.log.service.ClickLogService;

@Component
@RequiredArgsConstructor
@JmsConsumer(targetType = "ClickLogReply")
public class ClickLogConsumer extends BaseConsumer {
  private static final Logger LOGGER = LogManager.getLogger();
  private final ObjectMapper objectMapper;
  private final ClickLogService clickLogService;

  @Override
  protected Mono<Void> consume(final Delivery message) {
    LOGGER.debug("Processing ClickLogReply message: {}", message);

    ClickLogReply model = convertMessage(message);
    return clickLogService.setLog(model);
  }

  private ClickLogReply convertMessage(Delivery message) {
    try {
      String json = new String(message.getBody(), StandardCharsets.UTF_8);
      return objectMapper.readValue(json, ClickLogReply.class);
    } catch (Exception e) {
      LOGGER.error("Failed to parse ClickLogReply: {}", e.getMessage());
      throw new RuntimeException("Error parsing ClickLogReply", e);
    }
  }
}
