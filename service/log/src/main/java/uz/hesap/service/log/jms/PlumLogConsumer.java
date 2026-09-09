package uz.hesap.service.log.jms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Delivery;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.message.PlumLogReply;
import uz.hesap.service.jms.BaseConsumer;
import uz.hesap.service.jms.JmsConsumer;
import uz.hesap.service.log.service.PlumLogService;

@Component
@RequiredArgsConstructor
@JmsConsumer(targetType = "PlumLogReply")
public class PlumLogConsumer extends BaseConsumer {
  private static final Logger LOGGER = LogManager.getLogger();
  private final ObjectMapper objectMapper;
  private final PlumLogService plumLogService;

  @Override
  protected Mono<Void> consume(final Delivery message) {
    LOGGER.debug("Processing PlumLogReply message: {}", message);

    PlumLogReply model = convertMessage(message);
    return plumLogService.setLog(model);
  }

  private PlumLogReply convertMessage(Delivery message) {
    try {
      String json = new String(message.getBody(), StandardCharsets.UTF_8);
      return objectMapper.readValue(json, PlumLogReply.class);
    } catch (Exception e) {
      LOGGER.error("Failed to parse PlumLogReply: {}", e.getMessage());
      throw new RuntimeException("Error parsing PlumLogReply", e);
    }
  }
}
