package uz.hesap.service.main.feign;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uz.hesap.service.main.model.TemplateApplicationResponse;
import uz.hesap.service.common.util.TemplateBasicResponse;

@Service
@Log4j2
public class DocumentServiceClient {

  private final WebClient webClient;

  public DocumentServiceClient(
      WebClient.Builder webClientBuilder,
      @Value("${application.document-service.base-url}") String documentServiceBaseUrl) {
    this.webClient = webClientBuilder.baseUrl(documentServiceBaseUrl).build();
  }

  public Mono<TemplateApplicationResponse> getTemplateApplication(UUID templateApplicationId) {
    return webClient
        .get()
        .uri("/document/v1/local/applications/{id}", templateApplicationId)
        .retrieve()
        .bodyToMono(TemplateApplicationResponse.class);
  }

  public Mono<Map<UUID, TemplateBasicResponse>> getTemplateNamesMap(List<UUID> ids) {
    return webClient
        .post()
        .uri("/document/v1/local/templates")
        .bodyValue(ids)
        .retrieve()
        .bodyToFlux(TemplateBasicResponse.class)
        .collectMap(TemplateBasicResponse::id, Function.identity());
  }
}
