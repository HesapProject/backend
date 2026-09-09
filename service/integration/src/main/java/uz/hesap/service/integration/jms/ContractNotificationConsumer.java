package uz.hesap.service.integration.jms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Delivery;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.message.ContractNotificationEvent;
import uz.hesap.service.common.util.message.FirebaseNotificationReply;
import uz.hesap.service.common.util.message.NotificationEvent;
import uz.hesap.service.integration.model.NotificationConfig;
import uz.hesap.service.integration.service.EskizProvider;
import uz.hesap.service.integration.service.NotificationService;
import uz.hesap.service.integration.service.TemplateNotificationService;
import uz.hesap.service.integration.webclient.UserServiceClient;
import uz.hesap.service.jms.BaseConsumer;
import uz.hesap.service.jms.JmsConsumer;

/**
 * Document servisdan kelgan {@link ContractNotificationEvent}'ni qabul qiladi va template
 * notification config'iga qarab mijozga Firebase push (yoqilgan bo'lsa) va SMS (yoqilgan bo'lsa)
 * yuboradi. Config yo'q bo'lsa default: push yoniq, SMS o'chiq.
 */
@Component
@RequiredArgsConstructor
@JmsConsumer(targetType = "ContractNotificationEvent")
public class ContractNotificationConsumer extends BaseConsumer {

  private static final Logger LOGGER = LogManager.getLogger();

  private final ObjectMapper objectMapper;
  private final TemplateNotificationService templateNotificationService;
  private final NotificationService notificationService;
  private final EskizProvider eskizProvider;
  private final UserServiceClient userServiceClient;

  @Override
  protected Mono<Void> consume(final Delivery message) {
    ContractNotificationEvent event = convert(message);
    if (event == null || event.event() == null || event.recipientUserId() == null) {
      return Mono.empty();
    }
    return templateNotificationService
        .resolve(event.templateId(), event.event())
        .flatMap(cfg -> sendPush(event, cfg).then(sendSms(event, cfg)))
        .onErrorResume(
            e -> {
              LOGGER.error(
                  "ContractNotificationEvent [{}] processing failed: {}",
                  event.event(),
                  e.getMessage());
              return Mono.empty();
            });
  }

  private Mono<Void> sendPush(ContractNotificationEvent ev, NotificationConfig cfg) {
    if (!cfg.firebaseEnabled()) {
      return Mono.empty();
    }
    FirebaseNotificationReply reply = buildPush(ev, cfg.firebaseText());
    if (reply == null) {
      return Mono.empty();
    }
    return notificationService
        .send(reply)
        .onErrorResume(
            e -> {
              LOGGER.error("Push send failed [{}]: {}", ev.event(), e.getMessage());
              return Mono.empty();
            });
  }

  private Mono<Void> sendSms(ContractNotificationEvent ev, NotificationConfig cfg) {
    if (!cfg.smsEnabled()) {
      return Mono.empty();
    }
    String raw =
        (cfg.smsText() != null && !cfg.smsText().isBlank())
            ? cfg.smsText()
            : defaultSmsBody(ev.event());
    if (raw == null || raw.isBlank()) {
      return Mono.empty();
    }
    String text =
        raw.replace("{raqam}", ev.docNumber() != null ? ev.docNumber() : "—")
            .replace("{yuboruvchi}", ev.senderName() != null ? ev.senderName() : "");
    return userServiceClient
        .getUserById(ev.recipientUserId())
        .filter(u -> u.phone() != null && !u.phone().isBlank())
        .flatMap(u -> eskizProvider.send(u.phone(), text).then())
        .onErrorResume(
            e -> {
              LOGGER.error("SMS send failed [{}]: {}", ev.event(), e.getMessage());
              return Mono.empty();
            });
  }

  /** Hodisa bo'yicha push xabari (config matni — customBody). */
  private FirebaseNotificationReply buildPush(ContractNotificationEvent ev, String customBody) {
    java.util.UUID docId = ev.documentId();
    java.util.UUID to = ev.recipientUserId();
    String number = ev.docNumber();
    return switch (ev.event()) {
      case DOCUMENT_CREATED ->
          FirebaseNotificationReply.documentCreated(docId, to, number, ev.senderName(), customBody);
      case CANCEL_REQUEST ->
          FirebaseNotificationReply.documentCancelled(
              docId, to, number, ev.senderName(), customBody);
      case PAYMENT_REQUEST ->
          FirebaseNotificationReply.paymentRequest(docId, to, number, customBody);
      case PAYMENT_DELAY ->
          FirebaseNotificationReply.paymentDelayRequest(docId, to, number, customBody);
      case NOTICE -> FirebaseNotificationReply.noticeCreated(docId, to, number, customBody);
      case CLAIM -> FirebaseNotificationReply.reportCreated(docId, to, number, customBody);
      case WITNESS -> FirebaseNotificationReply.witnessInvited(docId, to, number, customBody);
    };
  }

  /** Hodisa bo'yicha default SMS matni ({raqam}/{yuboruvchi} placeholder bilan). */
  private String defaultSmsBody(NotificationEvent event) {
    return switch (event) {
      case DOCUMENT_CREATED ->
          "Hesap.uz ilovasida sizga {raqam}-sonli shartnoma imzolash uchun yuborildi.";
      case CANCEL_REQUEST -> "{yuboruvchi} shartnoma №{raqam} ni bekor qildi";
      case PAYMENT_REQUEST -> "Shartnoma №{raqam} bo'yicha to'lov so'rovi keldi";
      case PAYMENT_DELAY -> "Shartnoma №{raqam} to'lov sanasini o'zgartirish so'rovi keldi";
      case NOTICE -> "Hamkoringiz shartnoma №{raqam} bo'yicha talabnoma jo'natdi";
      case CLAIM -> "Hamkoringiz shartnoma №{raqam} bo'yicha da'vo arizasini jo'natdi";
      case WITNESS -> "Sizni shartnoma №{raqam} ga guvohlikka chaqirishmoqda";
    };
  }

  private ContractNotificationEvent convert(Delivery message) {
    try {
      String json = new String(message.getBody(), StandardCharsets.UTF_8);
      return objectMapper.readValue(json, ContractNotificationEvent.class);
    } catch (Exception e) {
      LOGGER.error("Failed to parse ContractNotificationEvent: {}", e.getMessage());
      return null;
    }
  }
}
