package uz.hesap.service.integration.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import uz.hesap.service.common.util.TextModel;
import uz.hesap.service.common.util.message.FirebaseNotificationReply;
import uz.hesap.service.common.util.message.FirebaseTopicReply;
import uz.hesap.service.common.util.message.NotificationType;
import uz.hesap.service.integration.webclient.UserServiceClient;

// FCM yuborishning yagona joyi (notification servisidan ko'chirildi).
// FirebaseMessaging bloklovchi → boundedElastic'da bajariladi (event-loop bloklanmaydi).
@Log4j2
@Service
@RequiredArgsConstructor
public class FirebaseProvider {

  private final ObjectMapper objectMapper;
  private final UserServiceClient userServiceClient;

  // Foydalanuvchiga push: token'lar berilgan bo'lsa o'shani, yo'q bo'lsa main-service'dan oladi.
  public Mono<Void> sendUser(FirebaseNotificationReply reply) {
    return resolveTokens(reply)
        .flatMap(
            tokens -> {
              if (tokens.isEmpty()) {
                log.debug("No tokens for userId [{}], skipping FCM", reply.toUserId());
                return Mono.empty();
              }
              return Mono.fromRunnable(
                      () -> sendMulticast(tokens, reply.dataId(), reply.type(), reply.title(),
                          reply.body()))
                  .subscribeOn(Schedulers.boundedElastic())
                  .then();
            });
  }

  // Topic'ga push (BLOG/ARTICLE).
  public Mono<Void> sendTopic(FirebaseTopicReply reply) {
    return Mono.fromRunnable(() -> sendToTopic(reply))
        .subscribeOn(Schedulers.boundedElastic())
        .then();
  }

  private Mono<List<String>> resolveTokens(FirebaseNotificationReply reply) {
    if (reply.tokens() != null && !reply.tokens().isEmpty()) {
      return Mono.just(reply.tokens());
    }
    if (Boolean.TRUE.equals(reply.tokensDeferred())) {
      return userServiceClient.getFirebaseTokens(reply.toUserId());
    }
    return Mono.just(List.of());
  }

  private void sendMulticast(
      List<String> tokens, java.util.UUID dataId, NotificationType type, TextModel title,
      TextModel body) {
    List<String> validTokens = tokens.stream().filter(t -> t != null && !t.isBlank()).toList();
    if (validTokens.isEmpty()) {
      log.warn("Tokens empty/blank — cannot send push.");
      return;
    }
    try {
      MulticastMessage message =
          MulticastMessage.builder()
              .setNotification(
                  Notification.builder()
                      .setTitle(title != null ? title.get(null) : null)
                      .setBody(body != null ? body.get(null) : null)
                      .build())
              .putAllData(buildDataPayload(null, dataId, type, title, body))
              .addAllTokens(validTokens)
              .setAndroidConfig(
                  AndroidConfig.builder().setPriority(AndroidConfig.Priority.HIGH).build())
              .setApnsConfig(apnsConfig())
              .build();

      BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);
      response.getResponses().stream()
          .filter(r -> r.getException() != null)
          .forEach(r -> log.error("FCM send error for a token: {}", r.getException().getMessage()));
    } catch (FirebaseMessagingException e) {
      log.error("FCM FirebaseMessagingException: {} - ErrorCode: {}", e.getMessage(),
          e.getErrorCode());
    } catch (Exception e) {
      log.error("FCM Unexpected Error: {}", e.getMessage(), e);
    }
  }

  private void sendToTopic(FirebaseTopicReply reply) {
    try {
      Message message =
          Message.builder()
              .putAllData(buildDataPayload(reply.id(), reply.dataId(), reply.type(), reply.title(),
                  reply.body()))
              .setAndroidConfig(
                  AndroidConfig.builder().setPriority(AndroidConfig.Priority.HIGH).build())
              .setApnsConfig(apnsConfig())
              .setTopic(reply.topic())
              .build();

      String response = FirebaseMessaging.getInstance().send(message);
      log.info("Successfully sent message to topic [{}]: {}", reply.topic(), response);
    } catch (Exception e) {
      log.error("Failed to send Firebase topic notification: {}", e.getMessage(), e);
    }
  }

  private ApnsConfig apnsConfig() {
    return ApnsConfig.builder()
        .setAps(
            Aps.builder()
                .setMutableContent(true)
                .setBadge(1)
                .setSound(CriticalSound.builder().setVolume(1.0).setName("default").build())
                .setContentAvailable(true)
                .build())
        .build();
  }

  private Map<String, String> buildDataPayload(
      java.util.UUID id, java.util.UUID dataId, NotificationType type, TextModel title,
      TextModel body) {
    Map<String, String> data = new HashMap<>();
    if (id != null) {
      data.put("id", id.toString());
    }
    if (dataId != null) {
      data.put("dataId", safeString(dataId));
    }
    if (type != null) {
      data.put("type", safeString(type));
    }
    if (title != null) {
      data.put("title", toJsonSafely(title.truncate(32)));
    }
    if (body != null) {
      data.put("body", toJsonSafely(body.truncate(64)));
    }
    data.put("data", String.valueOf(System.currentTimeMillis()));
    return data;
  }

  private String safeString(Object value) {
    return value != null && !value.equals("null") ? String.valueOf(value) : "";
  }

  private String toJsonSafely(Object value) {
    if (value == null) return "";
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      log.error("Error serializing JSON for FCM data payload", e);
      return "";
    }
  }
}
