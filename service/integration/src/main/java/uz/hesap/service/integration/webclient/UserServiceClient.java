package uz.hesap.service.integration.webclient;

import java.util.List;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.UserIdsRequest;
import uz.hesap.service.common.util.UserResponse;
import uz.hesap.service.integration.context.WebClientConfig;

// FCM token'larni main-service'dan olish (push tokens deferred bo'lganda).
// Bean nomi aniq berilgan — service.client.UserServiceClient bilan default
// "userServiceClient" nomida to'qnashmasin (ConflictingBeanDefinition →
// integration-service ishga tushmasdi, crash-loop). Inject TYPE bo'yicha.
@Log4j2
@Service("firebaseUserServiceClient")
public class UserServiceClient {

  private final WebClient webClient;

  public UserServiceClient(
      WebClient.Builder webClientBuilder,
      @Value("${application.main-service.base-url}") String baseUrl) {
    this.webClient =
        webClientBuilder.baseUrl(baseUrl).filter(WebClientConfig.errorHandler()).build();
  }

  // FCM tokenlar — local endpoint PINFL bo'yicha (GET /local/sessions?in=). userId'dan
  // avval PINFL resolve qilinadi (POST /local/users).
  public Mono<List<String>> getFirebaseTokens(UUID userId) {
    if (userId == null) {
      return Mono.just(List.of());
    }
    log.debug("Fetching firebase tokens for userId: {}", userId);
    return resolveInById(userId)
        .flatMap(
            in ->
                webClient
                    .get()
                    .uri(b -> b.path("/main/v1/local/sessions").queryParam("in", in).build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<String>>() {}))
        .defaultIfEmpty(List.of());
  }

  // userId bo'yicha to'liq UserResponse (phone, ism va h.k.) — POST /local/users.
  public Mono<UserResponse> getUserById(UUID userId) {
    if (userId == null) {
      return Mono.empty();
    }
    return webClient
        .post()
        .uri("/main/v1/local/users")
        .bodyValue(new UserIdsRequest(List.of(userId)))
        .retrieve()
        .bodyToFlux(UserResponse.class)
        .next();
  }

  // PINFL/IN bo'yicha main-service'dan userId resolve (admin tab'lari uchun).
  public Mono<UUID> resolveUserIdByPinfl(String pinfl) {
    if (pinfl == null || pinfl.isBlank()) {
      return Mono.empty();
    }
    return webClient
        .get()
        .uri("/main/v1/local/users/{in}", pinfl)
        .retrieve()
        .bodyToMono(UserIdProjection.class)
        .map(UserIdProjection::id)
        .onErrorResume(e -> Mono.empty());
  }

  // userId → PINFL/STIR (in) — POST /local/users orqali.
  private Mono<String> resolveInById(UUID userId) {
    return webClient
        .post()
        .uri("/main/v1/local/users")
        .bodyValue(new UserIdsRequest(List.of(userId)))
        .retrieve()
        .bodyToFlux(UserInProjection.class)
        .next()
        .mapNotNull(UserInProjection::in);
  }

  // main UserResponse'dan kerakli maydon (qolgani e'tiborsiz qoldiriladi).
  private record UserIdProjection(UUID id) {}

  private record UserInProjection(String in) {}

  // Scoring usage yozish (main /local/scoring-usage) — best-effort, oqimni buzmaydi.
  public reactor.core.publisher.Mono<Void> recordScoringUsage(
      String userIn, java.util.UUID packageId, String scoringType, String scoringRef) {
    return webClient
        .post()
        .uri("/main/v1/local/scoring-usage")
        .bodyValue(new ScoringUsageBody(userIn, packageId, scoringType, scoringRef))
        .retrieve()
        .bodyToMono(Void.class)
        .onErrorResume(e -> reactor.core.publisher.Mono.empty());
  }

  // Umumlashgan scoring jurnaliga yozish (usage EMAS — jamlash). Best-effort.
  public reactor.core.publisher.Mono<Void> recordUserScoring(
      String scoringId, String scoringType, String requesterIn, String userIn) {
    return webClient
        .post()
        .uri("/main/v1/local/user-scoring")
        .bodyValue(new UserScoringBody(scoringId, scoringType, requesterIn, userIn))
        .retrieve()
        .bodyToMono(Void.class)
        .onErrorResume(e -> reactor.core.publisher.Mono.empty());
  }

  public record UserScoringBody(
      String scoringId, String scoringType, String requesterIn, String userIn) {}

  public record ScoringUsageBody(
      String userIn, java.util.UUID packageId, String scoringType, String scoringRef) {}
}
