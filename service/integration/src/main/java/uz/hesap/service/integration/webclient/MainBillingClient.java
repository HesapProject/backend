package uz.hesap.service.integration.webclient;

import java.util.UUID;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uz.hesap.service.integration.context.WebClientConfig;

// Integration → main s2s: to'lov tasdiqlangach paketni biriktirish (grant).
// main'dagi purchasePackage `method=BALANCE` da endigina to'ldirilgan balansdan
// yechadi (net nol) va user_package + xarid tarixini yozadi.
@Log4j2
@Service
public class MainBillingClient {

  private final WebClient webClient;

  public MainBillingClient(
      WebClient.Builder webClientBuilder,
      @Value("${application.main-service.base-url}") String baseUrl) {
    this.webClient =
        webClientBuilder.baseUrl(baseUrl).filter(WebClientConfig.errorHandler()).build();
  }

  public Mono<Void> grantPackage(
      String userIn, UUID userId, UUID packageId, String promoCode, String method) {
    return webClient
        .post()
        .uri("/main/v1/local/packages/grant")
        .bodyValue(new GrantRequest(userIn, userId, packageId, promoCode, method))
        .retrieve()
        .bodyToMono(Void.class);
  }

  public record GrantRequest(
      String userIn, UUID userId, UUID packageId, String promoCode, String method) {}
}
