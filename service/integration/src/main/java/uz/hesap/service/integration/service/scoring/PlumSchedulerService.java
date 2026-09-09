package uz.hesap.service.integration.service.scoring;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.exception.BadRequestException;
import uz.hesap.service.integration.domain.enums.ScoringStatus;
import uz.hesap.service.integration.model.plum.ScoringGetPointResponse;
import uz.hesap.service.integration.repository.PlumScoringRepository;
import uz.hesap.service.integration.service.PlumSettingService;

// Plum scoring status'ini har 60 sekundda tekshiradi. Creds DB'dan
// (plum_setting jadvali) o'qiladi — har tick'da yangi WebClient quriladi
// (settings o'zgarsa darhol qo'llaniladi).
@Log4j2
@Service
public class PlumSchedulerService {

  private final WebClient.Builder webClientBuilder;
  private final PlumSettingService plumSettingService;
  private final PlumScoringRepository scoringRepository;
  private final ObjectMapper objectMapper;

  public PlumSchedulerService(
      WebClient.Builder webClientBuilder,
      PlumSettingService plumSettingService,
      PlumScoringRepository scoringRepository,
      ObjectMapper objectMapper) {
    this.webClientBuilder = webClientBuilder;
    this.plumSettingService = plumSettingService;
    this.scoringRepository = scoringRepository;
    this.objectMapper = objectMapper;
  }

  private Mono<WebClient> plumWebClient() {
    return plumSettingService
        .getCurrent()
        .handle(
            (setting, sink) -> {
              if (setting.getBaseUrl() == null
                  || setting.getLogin() == null
                  || setting.getPassword() == null) {
                sink.error(new BadRequestException("Plum credentials not configured"));
                return;
              }
              String auth = setting.getLogin() + ":" + setting.getPassword();
              String authHeader =
                  "Basic "
                      + Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
              sink.next(
                  webClientBuilder
                      .baseUrl(setting.getBaseUrl())
                      .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                      .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                      .defaultHeader(HttpHeaders.AUTHORIZATION, authHeader)
                      .build());
            })
        .cast(WebClient.class);
  }

  @Scheduled(fixedRate = 60000)
  public void checkScoringStatus() {
    plumWebClient()
        .flatMapMany(
            client ->
                scoringRepository
                    .findAllByStatus(ScoringStatus.IN_PROGRESS)
                    .flatMap(entity -> processOne(client, entity)))
        .onErrorResume(
            e -> {
              // Creds sozlanmagan bo'lsa, scheduler tekshirib o'tmaydi — keyingi
              // tick'da yana urinadi.
              log.warn("Plum scheduler skipped: {}", e.getMessage());
              return Mono.empty();
            })
        .subscribe();
  }

  private Mono<?> processOne(
      WebClient client, uz.hesap.service.integration.domain.PlumScoringEntity entity) {
    return client
        .get()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path("/Scoring/scoringGetPoint")
                    .queryParam("scoringId", entity.getPlumScoringId())
                    .build())
        .retrieve()
        .onStatus(
            HttpStatusCode::is4xxClientError,
            clientResponse -> {
              log.error("Error plum : {}", clientResponse.statusCode());
              return clientResponse
                  .bodyToMono(String.class)
                  .flatMap(
                      errorBody -> {
                        try {
                          ScoringGetPointResponse error =
                              objectMapper.readValue(errorBody, ScoringGetPointResponse.class);
                          return Mono.error(new BadRequestException(error.error().errorMessage()));
                        } catch (JsonProcessingException e) {
                          return Mono.error(new RuntimeException("PLUM Error: " + errorBody));
                        }
                      });
            })
        .bodyToMono(ScoringGetPointResponse.class)
        .flatMap(
            response -> {
              if (response.result() != null) {
                Map<String, Object> map =
                    objectMapper.convertValue(response, new TypeReference<>() {});
                entity.setHumo(map);
                entity.setStatus(ScoringStatus.COMPLETED);
                return scoringRepository.save(entity);
              }
              return Mono.empty();
            })
        .onErrorResume(
            e -> {
              log.error("Error checking status for scoringId: {}", entity.getPlumScoringId(), e);
              return Mono.empty();
            });
  }
}
