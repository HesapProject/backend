package uz.hesap.service.integration.service.pochta;

import java.util.Map;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.integration.domain.PochtaLogEntity;
import uz.hesap.service.integration.domain.PochtaSettingEntity;
import uz.hesap.service.integration.model.PochtaLogResponse;
import uz.hesap.service.integration.model.PochtaTestResult;
import uz.hesap.service.integration.repository.PochtaLogRepository;

// hybrid.pochta.uz bilan HTTP aloqa: OAuth2 password grant orqali token olish
// (/token), monitoring uchun har chaqiruv pochta_log'ga yoziladi.
@Service
@Log4j2
public class PochtaService {

  private final PochtaSettingService settingService;
  private final PochtaLogRepository logRepository;
  private final WebClient webClient;

  public PochtaService(
      PochtaSettingService settingService,
      PochtaLogRepository logRepository,
      WebClient.Builder webClientBuilder) {
    this.settingService = settingService;
    this.logRepository = logRepository;
    this.webClient = webClientBuilder.build();
  }

  private static String base(PochtaSettingEntity c) {
    String u = c.getBaseUrl() == null ? "https://hybrid.pochta.uz/" : c.getBaseUrl().trim();
    return u.endsWith("/") ? u.substring(0, u.length() - 1) : u;
  }

  // POST /token — access_token oladi. Har urinish log'ga yoziladi.
  @SuppressWarnings("unchecked")
  public Mono<String> getToken() {
    return settingService
        .getCurrent()
        .flatMap(
            c ->
                webClient
                    .post()
                    .uri(base(c) + "/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(
                        BodyInserters.fromFormData("grant_type", "password")
                            .with("username", c.getUsername() == null ? "" : c.getUsername())
                            .with("password", c.getPassword() == null ? "" : c.getPassword()))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .flatMap(
                        (Map body) -> {
                          Object tok = body.get("access_token");
                          if (tok == null) {
                            return log("TOKEN", "ERROR", null, null, "access_token yo'q", body.toString())
                                .then(Mono.error(new RuntimeException("access_token yo'q")));
                          }
                          return log("TOKEN", "SUCCESS", null, null, null, "ok")
                              .thenReturn(tok.toString());
                        })
                    .onErrorResume(
                        e ->
                            log("TOKEN", "ERROR", null, null, e.getMessage(), null)
                                .then(Mono.error(e))));
  }

  // Admin UI "Test ulanish" — token olishga urinadi, natijani qaytaradi.
  public Mono<PochtaTestResult> testConnection() {
    return getToken()
        .map(t -> new PochtaTestResult(true, "Ulanish muvaffaqiyatli — token olindi"))
        .onErrorResume(
            e -> Mono.just(new PochtaTestResult(false, "Xatolik: " + e.getMessage())));
  }

  public Flux<PochtaLogResponse> logs(int page, int size) {
    return logRepository
        .findAllByOrderByCreatedAtDesc(PageRequest.of(page, size))
        .map(
            l ->
                new PochtaLogResponse(
                    l.getId(),
                    l.getAction(),
                    l.getStatus(),
                    l.getMailId(),
                    l.getRequest(),
                    l.getResponse(),
                    l.getErrorMessage(),
                    l.getCreatedAt()));
  }

  private Mono<Void> log(
      String action, String status, String mailId, String request, String error, String response) {
    PochtaLogEntity e = new PochtaLogEntity();
    e.setAction(action);
    e.setStatus(status);
    e.setMailId(mailId);
    e.setRequest(request);
    e.setResponse(response);
    e.setErrorMessage(error);
    return logRepository.save(e).then();
  }
}
