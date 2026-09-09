package uz.hesap.service.integration.service.client;

import java.util.UUID;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.exception.NotFoundException;
import uz.hesap.service.integration.domain.enums.BillingType;

// User servisning internal (local) endpointlariga chaqiruvlar.
@Service
@Log4j2
public class UserServiceClient {

  private final WebClient webClient;

  public UserServiceClient(
      WebClient.Builder webClientBuilder,
      @Value("${application.main-service.base-url}") String userServiceBaseUrl) {
    this.webClient = webClientBuilder.baseUrl(userServiceBaseUrl).build();
  }

  // UUID bo'yicha billing turini aniqlaydi (Payme/Click to'lov oqimida ishlatiladi).
  public Mono<BillingType> determineBillingTypeByUUID(UUID uuid) {
    log.debug("Determining billing type by id: {}", uuid);
    return webClient
        .get()
        .uri("/main/v1/local/{uniqueId}/type", uuid)
        .retrieve()
        .bodyToMono(String.class)
        .map(
            response -> {
              if ("CLIENT".equals(response)) {
                log.debug("User {} is C2C (CLIENT)", uuid);
                return BillingType.C2C;
              }
              if ("COMPANY".equals(response)) {
                log.debug("User {} is B2B", uuid);
                return BillingType.B2B;
              }
              throw new NotFoundException("Invalid UUID");
            });
  }
}
