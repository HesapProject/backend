package uz.hesap.service.log.jms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Delivery;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.message.MyIdLogReply;
import uz.hesap.service.jms.BaseConsumer;
import uz.hesap.service.jms.JmsConsumer;
import uz.hesap.service.log.service.MyIdLogService;

@Component
@RequiredArgsConstructor
@JmsConsumer(targetType = "MyIdLogReply")
public class MyIdLogConsumer extends BaseConsumer {
  private static final Logger LOGGER = LogManager.getLogger();
  private final ObjectMapper objectMapper;
  private final MyIdLogService myIdLogService;

  @Override
  protected Mono<Void> consume(final Delivery message) {
    LOGGER.debug("Processing MyIdLogReply message: {}", message);

    MyIdLogReply model = convertMessage(message);
    return myIdLogService.setLog(model);
  }

  private MyIdLogReply convertMessage(Delivery message) {
    try {
      String json = new String(message.getBody(), StandardCharsets.UTF_8);
      return objectMapper.readValue(json, MyIdLogReply.class);
    } catch (Exception e) {
      LOGGER.error("Failed to parse MyIdLogReply: {}", e.getMessage());
      throw new RuntimeException("Error parsing MyIdLogReply", e);
    }
  }
}
