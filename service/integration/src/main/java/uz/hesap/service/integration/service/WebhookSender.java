package uz.hesap.service.integration.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HexFormat;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import uz.hesap.service.common.util.WebhookTargetResponse;
import uz.hesap.service.common.util.message.WebhookEvent;

/**
 * Hamkorning webhook URL'iga hodisani yetkazadi.
 *
 * <p>Har bir so'rov headerlari: `X-Hesap-Event` (hodisa turi), `X-Hesap-Delivery` (unikal id),
 * `X-Hesap-Signature` — `sha256=<hex>`, ya'ni body'ning kalit webhook secret'i bilan HMAC-SHA256
 * imzosi. Qabul qiluvchi tomon shu imzoni qayta hisoblab so'rov haqiqiyligini tekshiradi.
 *
 * <p>Yetkazib bo'lmasa 3 marta (2s dan boshlab, eksponensial) qayta uriniladi; baribir
 * bo'lmasa faqat log yoziladi — hodisa yo'qoladi (at-most-once).
 */
@Log4j2
@Service
public class WebhookSender {

  private static final String HMAC_ALGORITHM = "HmacSHA256";
  private static final Duration TIMEOUT = Duration.ofSeconds(10);
  private static final int MAX_ATTEMPTS = 3;

  private final WebClient webClient;
  private final ObjectMapper objectMapper;

  public WebhookSender(final WebClient.Builder webClientBuilder, final ObjectMapper objectMapper) {
    // baseUrl yo'q — har safar hamkorning to'liq URL'i beriladi.
    this.webClient = webClientBuilder.build();
    this.objectMapper = objectMapper;
  }

  public Mono<Void> send(final WebhookTargetResponse target, final WebhookEvent event) {
    final String body;
    try {
      body = objectMapper.writeValueAsString(event);
    } catch (JsonProcessingException e) {
      log.error("WebhookEvent JSON'ga aylanmadi: {}", e.getMessage());
      return Mono.empty();
    }

    final String deliveryId = UUID.randomUUID().toString();
    return webClient
        .post()
        .uri(target.url())
        .contentType(MediaType.APPLICATION_JSON)
        .header("X-Hesap-Event", event.event().name())
        .header("X-Hesap-Delivery", deliveryId)
        .header("X-Hesap-Signature", "sha256=" + sign(body, target.secret()))
        .bodyValue(body)
        .retrieve()
        .toBodilessEntity()
        .timeout(TIMEOUT)
        .retryWhen(Retry.backoff(MAX_ATTEMPTS - 1, Duration.ofSeconds(2)))
        .doOnSuccess(
            response ->
                log.info(
                    "Webhook yetkazildi [{}] key={} -> {} ({})",
                    event.event(),
                    target.keyId(),
                    target.url(),
                    response.getStatusCode()))
        .onErrorResume(
            e -> {
              log.warn(
                  "Webhook yetkazilmadi [{}] key={} -> {}: {}",
                  event.event(),
                  target.keyId(),
                  target.url(),
                  e.getMessage());
              return Mono.empty();
            })
        .then();
  }

  // HMAC-SHA256(body, secret) -> hex. Secret bo'lmasa bo'sh imzo (eski kalitlar uchun).
  private String sign(final String body, final String secret) {
    if (secret == null || secret.isBlank()) {
      return "";
    }
    try {
      Mac mac = Mac.getInstance(HMAC_ALGORITHM);
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
      return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      log.error("Webhook imzosini hisoblab bo'lmadi: {}", e.getMessage());
      return "";
    }
  }
}
