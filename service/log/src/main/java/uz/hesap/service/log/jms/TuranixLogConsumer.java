package uz.hesap.service.log.jms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Delivery;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.message.TuranixLogReply;
import uz.hesap.service.jms.BaseConsumer;
import uz.hesap.service.jms.JmsConsumer;
import uz.hesap.service.log.service.TuranixLogService;

@Component
@RequiredArgsConstructor
@JmsConsumer(targetType = "TuranixLogReply")
public class TuranixLogConsumer extends BaseConsumer {
  private static final Logger LOGGER = LogManager.getLogger();
  private final ObjectMapper objectMapper;
  private final TuranixLogService turanixLogService;

  @Override
  protected Mono<Void> consume(final Delivery message) {
    LOGGER.debug("Processing TuranixLogReply message: {}", message);

    TuranixLogReply model = convertMessage(message);
    return turanixLogService.setLog(model);
  }

  private TuranixLogReply convertMessage(Delivery message) {
    try {
      String json = new String(message.getBody(), StandardCharsets.UTF_8);
      return objectMapper.readValue(json, TuranixLogReply.class);
    } catch (Exception e) {
      LOGGER.error("Failed to parse TuranixLogReply: {}", e.getMessage());
      throw new RuntimeException("Error parsing TuranixLogReply", e);
    }
  }
}
