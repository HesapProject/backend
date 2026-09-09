package uz.hesap.service.log.jms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Delivery;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.message.OneIdLogReply;
import uz.hesap.service.jms.BaseConsumer;
import uz.hesap.service.jms.JmsConsumer;
import uz.hesap.service.log.service.OneIdLogService;

@Component
@RequiredArgsConstructor
@JmsConsumer(targetType = "OneIdLogReply")
public class OneIdLogConsumer extends BaseConsumer {
  private static final Logger LOGGER = LogManager.getLogger();
  private final ObjectMapper objectMapper;
  private final OneIdLogService oneIdLogService;

  @Override
  protected Mono<Void> consume(final Delivery message) {
    LOGGER.debug("Processing OneIdLogReply message: {}", message);

    OneIdLogReply model = convertMessage(message);
    return oneIdLogService.setLog(model);
  }

  private OneIdLogReply convertMessage(Delivery message) {
    try {
      String json = new String(message.getBody(), StandardCharsets.UTF_8);
      return objectMapper.readValue(json, OneIdLogReply.class);
    } catch (Exception e) {
      LOGGER.error("Failed to parse UserLogReason: {}", e.getMessage());
      throw new RuntimeException("Error parsing UserLogReason", e);
    }
  }
}
