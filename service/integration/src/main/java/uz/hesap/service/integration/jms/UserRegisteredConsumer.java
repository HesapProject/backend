package uz.hesap.service.integration.jms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Delivery;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.message.UserRegisteredEvent;
import uz.hesap.service.integration.model.amo.AmoContactCommand;
import uz.hesap.service.integration.service.amo.AmoContactService;
import uz.hesap.service.jms.BaseConsumer;
import uz.hesap.service.jms.JmsConsumer;

// Yangi foydalanuvchi ro'yxatdan o'tganda (UserRegisteredEvent, RabbitMQ) amoCRM'ga
// kontakt yaratadi (eski crm-app addToCrm o'rniga). Fire-and-forget: amoCRM xatosi
// registratsiyaga ta'sir qilmaydi (event allaqachon publish qilingan).
@Component
@RequiredArgsConstructor
@JmsConsumer(targetType = "UserRegisteredEvent")
public class UserRegisteredConsumer extends BaseConsumer {

  private static final Logger LOGGER = LogManager.getLogger();

  private final ObjectMapper objectMapper;
  private final AmoContactService amoContactService;

  @Override
  protected Mono<Void> consume(final Delivery message) {
    final UserRegisteredEvent event = parse(message);
    if (event == null || event.userId() == null) {
      return Mono.empty();
    }
    final AmoContactCommand command =
        new AmoContactCommand(
            buildName(event), event.firstName(), event.lastName(), event.phone(), event.userId());
    return amoContactService
        .addContact(command)
        .doOnSuccess(r -> LOGGER.info("amoCRM contact for user {} -> {}", event.userId(), r))
        .onErrorResume(
            e -> {
              LOGGER.warn("amoCRM contact push failed for user {}: {}", event.userId(), e.getMessage());
              return Mono.empty();
            })
        .then();
  }

  private String buildName(UserRegisteredEvent event) {
    String full =
        Stream.of(event.firstName(), event.lastName())
            .filter(s -> s != null && !s.isBlank())
            .collect(Collectors.joining(" "))
            .trim();
    return full.isBlank() ? event.in() : full;
  }

  private UserRegisteredEvent parse(Delivery message) {
    try {
      String json = new String(message.getBody(), StandardCharsets.UTF_8);
      return objectMapper.readValue(json, UserRegisteredEvent.class);
    } catch (Exception e) {
      LOGGER.error("Failed to parse UserRegisteredEvent: {}", e.getMessage());
      return null;
    }
  }
}
