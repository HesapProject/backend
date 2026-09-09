package uz.hesap.service.log.jms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Delivery;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.message.ActivityLog;
import uz.hesap.service.jms.BaseConsumer;
import uz.hesap.service.jms.JmsConsumer;
import uz.hesap.service.log.service.ActivityLogService;

@Component
@RequiredArgsConstructor
@JmsConsumer(targetType = "ActivityLog")
public class ActivityLogConsumer extends BaseConsumer {
  private static final Logger LOGGER = LogManager.getLogger();
  private final ObjectMapper objectMapper;
  private final ActivityLogService activityLogService;

  @Override
  protected Mono<Void> consume(final Delivery message) {
    LOGGER.debug("Processing ActivityLog message: {}", message);
    ActivityLog model = convertMessage(message);
    return activityLogService.setLog(model);
  }

  private ActivityLog convertMessage(Delivery message) {
    try {
      String json = new String(message.getBody(), StandardCharsets.UTF_8);
      return objectMapper.readValue(json, ActivityLog.class);
    } catch (Exception e) {
      LOGGER.error("Failed to parse ActivityLog: {}", e.getMessage());
      throw new RuntimeException("Error parsing ActivityLog", e);
    }
  }
}
