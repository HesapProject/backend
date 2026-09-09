package uz.hesap.service.integration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import java.time.Instant;
import java.util.HashMap;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import uz.hesap.service.common.exception.InvalidOperationException;
import uz.hesap.service.common.util.message.EskizLogReply;
import uz.hesap.service.integration.model.eskiz.EskizAccountRequest;
import uz.hesap.service.integration.model.eskiz.EskizMessageRequest;
import uz.hesap.service.integration.model.eskiz.EskizResponse;
import uz.hesap.service.jms.JmsPublisher;

@Log4j2
@Service
public class EskizProvider {

  private static final String TOKEN_KEY = "HESAP_ESKIZ";

  private final WebClient eskizServiceClient;
  private final Cache<String, String> cachedEskiz;
  private final JmsPublisher jmsPublisher;
  private final SmsProviderSettingService smsProviderSettingService;
  private final ObjectMapper objectMapper;

  public EskizProvider(
      final WebClient.Builder webClientBuilder,
      @Qualifier("cachedEskiz") final Cache<String, String> cachedEskiz,
      @Value("${application.eskiz.base-url}") final String baseUrl,
      JmsPublisher jmsPublisher,
      SmsProviderSettingService smsProviderSettingService,
      ObjectMapper objectMapper) {
    this.cachedEskiz = cachedEskiz;
    this.eskizServiceClient = webClientBuilder.baseUrl(baseUrl).build();
    this.jmsPublisher = jmsPublisher;
    this.smsProviderSettingService = smsProviderSettingService;
    this.objectMapper = objectMapper;
  }

  public Mono<String> auth() {
    var token = cachedEskiz.getIfPresent(TOKEN_KEY);
    if (token != null) {
      return Mono.just(token);
    }

    return smsProviderSettingService
        .get()
        .flatMap(
            settings -> {
              if (settings.eskizEmail() == null || settings.eskizSecret() == null) {
                return Mono.error(
                    new InvalidOperationException("Eskiz credentials not configured"));
              }
              return eskizServiceClient
                  .post()
                  .uri("/auth/login")
                  .bodyValue(new EskizAccountRequest(settings.eskizEmail(), settings.eskizSecret()))
                  .retrieve()
                  // BUG FIX #2: surface the real Eskiz error body instead of a raw
                  // WebClientResponseException ("404 NOT_FOUND") that hid the cause.
                  .onStatus(HttpStatusCode::isError, this::toEskizError)
                  .bodyToMono(EskizResponse.class)
                  .map(response -> "Bearer " + response.data().token())
                  .doOnNext(t -> cachedEskiz.put(TOKEN_KEY, t))
                  .doOnNext(t -> log.info("ESKIZ token cached"))
                  .onErrorResume(
                      ex -> {
                        log.error("Eskiz authentication failed: {}", ex.getMessage(), ex);
                        return Mono.error(new InvalidOperationException("Eskiz auth failed", ex));
                      });
            });
  }

  public Mono<EskizResponse> send(final String phone, final String content) {
    // Yuboruvchi nickname sozlamadan (Eskiz'da tasdiqlangan "HESAP"); bo'sh bo'lsa "4546".
    return smsProviderSettingService
        .get()
        .map(s -> resolveFrom(s.eskizFrom()))
        .flatMap(from -> sendWith(phone, content, from));
  }

  private Mono<EskizResponse> sendWith(
      final String phone, final String content, final String from) {
    return auth()
        .flatMap(
            token ->
                eskizServiceClient
                    .post()
                    .uri("/message/sms/send")
                    .header("Authorization", token)
                    .body(
                        Mono.just(new EskizMessageRequest(phone, content, from)),
                        EskizMessageRequest.class)
                    .retrieve()
                    // BUG FIX #2: reveal Eskiz's actual error message.
                    .onStatus(HttpStatusCode::isError, this::toEskizError)
                    .bodyToMono(EskizResponse.class))
        .publishOn(Schedulers.boundedElastic())
        .doOnSuccess(
            response -> {
              log.info("Successfully sent SMS to {}. Response: {}", phone, response);
              var logReply =
                  new EskizLogReply(
                      phone,
                      content,
                      Boolean.FALSE,
                      null,
                      Instant.now(),
                      eskizRequestJson(phone, content, from),
                      toJson(response));
              jmsPublisher.publish(logReply).subscribe();
            })
        .doOnError(
            error -> {
              log.error("Failed to send SMS to {}. Error: {}", phone, error.getMessage());
              var logReply =
                  new EskizLogReply(
                      phone,
                      content,
                      Boolean.TRUE,
                      error.getMessage(),
                      Instant.now(),
                      eskizRequestJson(phone, content, from),
                      null);
              jmsPublisher.publish(logReply).subscribe();
            });
  }

  // Maps a non-2xx Eskiz response into a meaningful error carrying the body.
  private Mono<Throwable> toEskizError(
      final org.springframework.web.reactive.function.client.ClientResponse response) {
    return response
        .bodyToMono(String.class)
        .defaultIfEmpty("")
        .map(
            body ->
                new InvalidOperationException(
                    "Eskiz error [" + response.statusCode().value() + "]: " + body));
  }

  // Eskiz'ga yuborilgan so'rov tanasini JSON sifatida tayyorlaydi (log uchun).
  private String eskizRequestJson(final String phone, final String content, final String from) {
    var map = new HashMap<String, Object>();
    map.put("phone", phone);
    map.put("message", content);
    map.put("from", from);
    return toJson(map);
  }

  // Sozlamadagi nickname bo'sh bo'lsa Eskiz standart sender'i "4546".
  private String resolveFrom(final String configured) {
    return (configured != null && !configured.isBlank()) ? configured : "4546";
  }

  // Obyektni JSON string'ga aylantiradi; xato bo'lsa null (log to'xtab qolmasin).
  private String toJson(final Object value) {
    if (value == null) return null;
    try {
      return objectMapper.writeValueAsString(value);
    } catch (final Exception e) {
      log.warn("Eskiz log JSON serialization failed: {}", e.getMessage());
      return null;
    }
  }
}
