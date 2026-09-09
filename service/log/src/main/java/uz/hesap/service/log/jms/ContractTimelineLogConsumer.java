package uz.hesap.service.log.jms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Delivery;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.message.ContractTimelineLog;
import uz.hesap.service.jms.BaseConsumer;
import uz.hesap.service.jms.JmsConsumer;
import uz.hesap.service.log.service.ContractTimelineLogService;

@Component
@RequiredArgsConstructor
@JmsConsumer(targetType = "ContractTimelineLog")
public class ContractTimelineLogConsumer extends BaseConsumer {
  private static final Logger LOGGER = LogManager.getLogger();
  private final ObjectMapper objectMapper;
  private final ContractTimelineLogService logService;

  @Override
  protected Mono<Void> consume(final Delivery message) {
    LOGGER.debug("Processing ContractTimelineLog message: {}", message);
    return logService.saveLog(convertMessage(message));
  }

  private ContractTimelineLog convertMessage(Delivery message) {
    try {
      String json = new String(message.getBody(), StandardCharsets.UTF_8);
      return objectMapper.readValue(json, ContractTimelineLog.class);
    } catch (Exception e) {
      LOGGER.error("Failed to parse ContractTimelineLog: {}", e.getMessage());
      throw new RuntimeException("Error parsing ContractTimelineLog", e);
    }
  }
}
