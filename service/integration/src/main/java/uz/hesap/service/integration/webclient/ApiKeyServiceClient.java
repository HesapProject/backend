package uz.hesap.service.integration.webclient;

import java.util.Collection;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import uz.hesap.service.common.util.WebhookTargetResponse;
import uz.hesap.service.integration.context.WebClientConfig;

// OpenAPI kalitlarining webhook manzillari main servisda saqlanadi — hodisa
// kelganda kimga yuborishni shu yerdan so'raymiz.
@Log4j2
@Service
public class ApiKeyServiceClient {

  private final WebClient webClient;

  public ApiKeyServiceClient(
      final WebClient.Builder webClientBuilder,
      @Value("${application.main-service.base-url}") final String baseUrl) {
    this.webClient =
        webClientBuilder.baseUrl(baseUrl).filter(WebClientConfig.errorHandler()).build();
  }

  // Berilgan taraflar (PINFL/STIR) bo'yicha aktiv webhook kalitlari.
  public Flux<WebhookTargetResponse> webhookTargets(final Collection<String> ins) {
    if (ins == null || ins.isEmpty()) {
      return Flux.empty();
    }
    return webClient
        .post()
        .uri("/main/v1/local/api-keys/webhooks")
        .bodyValue(new WebhookTargetsBody(List.copyOf(ins)))
        .retrieve()
        .bodyToFlux(WebhookTargetResponse.class)
        .onErrorResume(
            e -> {
              log.warn("Webhook manzillarini olishda xato: {}", e.getMessage());
              return Flux.empty();
            });
  }

  public record WebhookTargetsBody(List<String> ins) {}
}
