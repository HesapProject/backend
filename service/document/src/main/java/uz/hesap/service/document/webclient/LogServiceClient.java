package uz.hesap.service.document.webclient;

import java.util.UUID;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uz.hesap.service.document.context.WebClientConfig;

// Log servisga s2s WebClient (payment reminder cron audit yozish uchun).
@Log4j2
@Service
public class LogServiceClient {

  private final WebClient webClient;

  @Autowired
  public LogServiceClient(
      final WebClient.Builder webClientBuilder,
      @Value("${application.log-service.base-url:}") final String baseUrl) {
    this.webClient =
        webClientBuilder.baseUrl(baseUrl).filter(WebClientConfig.errorHandler()).build();
  }

  // Cron boshlanganda audit yozuv yaratadi, id qaytadi.
  public Mono<UUID> startPaymentReminder() {
    return webClient
        .post()
        .uri("/log/v1/local/payment-reminder/start")
        .retrieve()
        .bodyToMono(java.util.Map.class)
        .map(m -> UUID.fromString(String.valueOf(m.get("id"))))
        .onErrorResume(
            e -> {
              log.error("Failed to start payment reminder log: {}", e.getMessage());
              return Mono.empty();
            });
  }

  // Cron yakuni.
  public Mono<Void> finishPaymentReminder(
      UUID id, int totalCandidates, int sentSuccess, int sentFailed, String errorMessage) {
    return webClient
        .post()
        .uri("/log/v1/local/payment-reminder/{id}/finish", id)
        .bodyValue(
            new PaymentReminderFinishRequest(
                totalCandidates, sentSuccess, sentFailed, errorMessage))
        .retrieve()
        .bodyToMono(Void.class)
        .onErrorResume(
            e -> {
              log.error("Failed to finish payment reminder log {}: {}", id, e.getMessage());
              return Mono.empty();
            });
  }

  private record PaymentReminderFinishRequest(
      Integer totalCandidates, Integer sentSuccess, Integer sentFailed, String errorMessage) {}
}
