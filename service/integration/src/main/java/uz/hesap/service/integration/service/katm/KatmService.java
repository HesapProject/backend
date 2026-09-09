package uz.hesap.service.integration.service.katm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.exception.BadRequestException;
import uz.hesap.service.common.exception.ForbiddenException;
import uz.hesap.service.common.exception.NotFoundException;
import uz.hesap.service.integration.domain.KatmReportEntity;
import uz.hesap.service.integration.domain.enums.KatmReportStatus;
import uz.hesap.service.integration.model.katm.CreditHistoryRequest;
import uz.hesap.service.integration.model.katm.CreditHistoryResponse;
import uz.hesap.service.integration.model.katm.KatmGetReportResponse;
import uz.hesap.service.integration.model.katm.KatmInitClientRequest;
import uz.hesap.service.integration.model.katm.KatmInitClientResponse;
import uz.hesap.service.integration.model.katm.KatmPriceResponse;
import uz.hesap.service.integration.model.katm.KatmSubmitResponse;
import uz.hesap.service.integration.model.mapper.KatmMapper;
import uz.hesap.service.integration.repository.KatmReportRepository;
import uz.hesap.service.integration.service.KatmSettingService;

// KATM kredit byurosi bilan ishlash. Oqim: init-client (pClientId) →
// submit-request (pClaimId + pToken, status REQUESTED) → keyin KatmSchedulerService
// get-report orqali reportBase64'ni to'ldiradi. Auth — KatmTokenService (Bearer JWT).
@Log4j2
@Service
public class KatmService {

  private final WebClient.Builder webClientBuilder;
  private final KatmSettingService settingService;
  private final KatmTokenService tokenService;
  private final KatmReportRepository reportRepository;
  private final KatmMapper katmMapper;
  private final ObjectMapper objectMapper;

  public KatmService(
      WebClient.Builder webClientBuilder,
      KatmSettingService settingService,
      KatmTokenService tokenService,
      KatmReportRepository reportRepository,
      KatmMapper katmMapper,
      ObjectMapper objectMapper) {
    this.webClientBuilder = webClientBuilder;
    this.settingService = settingService;
    this.tokenService = tokenService;
    this.reportRepository = reportRepository;
    this.katmMapper = katmMapper;
    this.objectMapper = objectMapper;
  }

  // Bearer token bilan WebClient quradi. Token KatmTokenService'da keshlanadi
  // (23 soat < 1 kun exp), shu sababli har so'rovda yangi login bo'lmaydi.
  Mono<WebClient> katmWebClient() {
    return Mono.zip(settingService.getCurrent(), tokenService.getAccessToken())
        .map(
            tuple ->
                webClientBuilder
                    .baseUrl(tuple.getT1().getBaseUrl())
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + tuple.getT2())
                    .build());
  }

  // ===================== Public oqim =====================

  // Kredit tarixini so'rashni boshlaydi: init-client → submit-request.
  // Natijada REQUESTED holatdagi yozuv qaytadi; hisobot scheduler orqali keladi.
  public Mono<CreditHistoryResponse> requestCreditHistory(CreditHistoryRequest request) {
    log.info("KATM credit history request, pinfl={}", request.pinfl());
    return initClient(request)
        .flatMap(
            pClientId ->
                submitReportRequest(pClientId, language(request.language()))
                    .flatMap(submit -> save(request, pClientId, submit)))
        .map(katmMapper::toResponse);
  }

  // init-client — mijozni KATM'da ro'yxatdan o'tkazib pClientId (KATM-SIR) oladi.
  public Mono<String> initClient(CreditHistoryRequest request) {
    KatmInitClientRequest body =
        new KatmInitClientRequest(
            request.pinfl(),
            request.docSeries(),
            request.docNumber(),
            request.firstName(),
            request.lastName(),
            request.middleName(),
            request.birthDate(),
            request.issueDocDate(),
            request.expiredDocDate(),
            request.gender(),
            request.districtId(),
            request.resAddress(),
            request.regAddress(),
            request.phone());
    return katmWebClient()
        .flatMap(
            client ->
                client
                    .post()
                    .uri("/auth/init-client")
                    .bodyValue(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, this::katmError)
                    .bodyToMono(KatmInitClientResponse.class))
        .flatMap(
            response -> {
              if (hasError(response.error())) {
                return Mono.error(new BadRequestException("KATM init-client: " + response.error().errMsg()));
              }
              if (response.data() == null || response.data().pClientId() == null) {
                return Mono.error(new BadRequestException("KATM init-client: pClientId kelmadi"));
              }
              log.info("KATM pClientId olindi");
              return Mono.just(response.data().pClientId());
            });
  }

  // Kredit tarixining narxi. URL KATM TZ'sida aniq ko'rsatilmagan — tasdiqlangach
  // moslang. Asosiy oqimni buzmaslik uchun alohida metod (ixtiyoriy chaqiriladi).
  public Mono<KatmPriceResponse> creditHistoryPrice(String pClientId) {
    return katmWebClient()
        .flatMap(
            client ->
                client
                    .post()
                    .uri("/report/price")
                    .bodyValue(Map.of("pClientId", pClientId))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, this::katmError)
                    .bodyToMono(KatmPriceResponse.class));
  }

  // submit-request — kredit tarixi arizasini yuboradi, pClaimId + pToken oladi.
  public Mono<KatmSubmitResponse> submitReportRequest(String pClientId, String language) {
    return katmWebClient()
        .flatMap(
            client ->
                client
                    .post()
                    .uri("/report/submit-request")
                    .bodyValue(Map.of("language", language, "pClientId", pClientId))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, this::katmError)
                    .bodyToMono(KatmSubmitResponse.class))
        .flatMap(
            response -> {
              if (hasError(response.error())) {
                return Mono.error(new BadRequestException("KATM submit-request: " + response.error().errMsg()));
              }
              if (response.data() == null || response.data().pClaimId() == null) {
                return Mono.error(new BadRequestException("KATM submit-request: pClaimId kelmadi"));
              }
              return Mono.just(response);
            });
  }

  // get-report — tayyor hisobotni Base64 XML ko'rinishida oladi. Scheduler chaqiradi.
  // Hisobot hali tayyor bo'lmasa reportBase64 null bo'lishi mumkin (qayta urinadi).
  public Mono<KatmGetReportResponse> getReport(String pClaimId, String pToken, String language) {
    return katmWebClient()
        .flatMap(
            client ->
                client
                    .post()
                    .uri("/report/get-report")
                    .bodyValue(Map.of("language", language, "pClaimId", pClaimId, "pToken", pToken))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, this::katmError)
                    .bodyToMono(KatmGetReportResponse.class));
  }

  // Bitta REQUESTED yozuvni tekshiradi; hisobot tayyor bo'lsa COMPLETED qiladi.
  public Mono<KatmReportEntity> pollPendingReport(KatmReportEntity entity) {
    return getReport(entity.getPClaimId(), entity.getPToken(), language(entity.getLanguage()))
        .flatMap(
            response -> {
              entity.setResponse(toMap(response));
              if (hasError(response.error())) {
                entity.setStatus(KatmReportStatus.FAILED);
                entity.setResultMessage(response.error().errMsg());
                entity.setCompletedAt(Instant.now());
                return reportRepository.save(entity);
              }
              if (response.data() != null && response.data().reportBase64() != null) {
                entity.setReportBase64(response.data().reportBase64());
                entity.setResultMessage(response.data().resultMessage());
                entity.setStatus(KatmReportStatus.COMPLETED);
                entity.setCompletedAt(Instant.now());
                return reportRepository.save(entity);
              }
              // Hali tayyor emas — keyingi tick'da yana tekshiriladi.
              return Mono.empty();
            })
        .onErrorResume(
            e -> {
              log.error("KATM get-report xatosi, claimId={}", entity.getPClaimId(), e);
              return Mono.empty();
            });
  }

  // ===================== O'qish =====================

  public Mono<CreditHistoryResponse> getById(UUID id) {
    return reportRepository
        .findById(id)
        .switchIfEmpty(Mono.error(new NotFoundException("KATM report not found")))
        .map(katmMapper::toResponse);
  }

  public Flux<CreditHistoryResponse> getHistory(UUID userId, String pinfl, Pageable pageable) {
    if (userId != null) {
      return reportRepository
          .findAllByUserIdOrderByCreatedAtDesc(userId, pageable)
          .map(katmMapper::toResponse);
    }
    if (pinfl != null && !pinfl.isBlank()) {
      return reportRepository
          .findAllByPinflOrderByCreatedAtDesc(pinfl, pageable)
          .map(katmMapper::toResponse);
    }
    return reportRepository.findAllByOrderByCreatedAtDesc(pageable).map(katmMapper::toResponse);
  }

  // ===================== Helpers =====================

  private Mono<KatmReportEntity> save(
      CreditHistoryRequest request, String pClientId, KatmSubmitResponse submit) {
    KatmReportEntity entity = new KatmReportEntity();
    entity.setUserId(request.userId());
    entity.setPinfl(request.pinfl());
    entity.setPClientId(pClientId);
    entity.setPClaimId(submit.data().pClaimId());
    entity.setPToken(submit.data().pToken());
    entity.setLanguage(language(request.language()));
    entity.setStatus(KatmReportStatus.REQUESTED);
    entity.setResultMessage(submit.data().resultMessage());
    entity.setRequest(toMap(request));
    entity.setCreatedAt(Instant.now());
    return reportRepository.save(entity);
  }

  private boolean hasError(uz.hesap.service.integration.model.katm.KatmError error) {
    return error != null && error.errId() != null && error.errId() != 0;
  }

  private String language(String language) {
    return (language == null || language.isBlank()) ? "uz" : language;
  }

  // KATM xato javobi: errId != 0 bo'lsa BadRequest, 403 bo'lsa Forbidden.
  private Mono<? extends Throwable> katmError(ClientResponse clientResponse) {
    if (clientResponse.statusCode().value() == 403) {
      return Mono.error(new ForbiddenException("KATM service'ga ruxsat yo'q"));
    }
    return clientResponse
        .bodyToMono(String.class)
        .defaultIfEmpty("")
        .flatMap(
            body -> {
              log.error("KATM error {}: {}", clientResponse.statusCode(), body);
              return Mono.error(
                  new BadRequestException("KATM xatosi: " + clientResponse.statusCode().value()));
            });
  }

  // Obyektni Map<String,Object> ga aylantiradi (JSONB ustun uchun); xato bo'lsa null.
  private Map<String, Object> toMap(Object value) {
    if (value == null) return null;
    try {
      return objectMapper.convertValue(value, new TypeReference<>() {});
    } catch (IllegalArgumentException e) {
      log.warn("KATM JSON convert failed: {}", e.getMessage());
      return null;
    }
  }
}
