package uz.hesap.service.integration.service.turanix;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
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
import uz.hesap.service.common.util.message.TuranixLogReply;
import uz.hesap.service.integration.domain.TuranixCheckEntity;
import uz.hesap.service.integration.domain.TuranixSettingEntity;
import uz.hesap.service.integration.model.mapper.TuranixMapper;
import uz.hesap.service.integration.model.turanix.CheckPassMsisdnRequest;
import uz.hesap.service.integration.model.turanix.CheckPassMsisdnResponse;
import uz.hesap.service.integration.model.turanix.TuranixCheckRequest;
import uz.hesap.service.integration.model.turanix.TuranixCheckResponse;
import uz.hesap.service.integration.repository.TuranixCheckRepository;
import uz.hesap.service.integration.service.TuranixSettingService;
import uz.hesap.service.jms.JmsPublisher;

// Turanix partnyor-servis bilan ishlash. Hozircha bitta feat: check-pass-msisdn —
// MSISDN'ning pasport/PINFL'ga biriktirilganini tekshirish. Auth — har so'rovda
// HMAC-SHA256 imzo (X-Timestamp/X-Nonce/X-Project-Id/X-Signature). Yangi feat
// qo'shilsa shu servisga metod qo'shiladi (Plum'dagidek).
@Log4j2
@Service
public class TuranixService {

  private static final String CHECK_PASS_MSISDN_PATH = "/v1/check-pass-msisdn";

  private final WebClient.Builder webClientBuilder;
  private final TuranixSettingService settingService;
  private final TuranixCheckRepository checkRepository;
  private final TuranixMapper turanixMapper;
  private final ObjectMapper objectMapper;
  private final JmsPublisher jmsPublisher;

  public TuranixService(
      WebClient.Builder webClientBuilder,
      TuranixSettingService settingService,
      TuranixCheckRepository checkRepository,
      TuranixMapper turanixMapper,
      ObjectMapper objectMapper,
      JmsPublisher jmsPublisher) {
    this.webClientBuilder = webClientBuilder;
    this.settingService = settingService;
    this.checkRepository = checkRepository;
    this.turanixMapper = turanixMapper;
    this.objectMapper = objectMapper;
    this.jmsPublisher = jmsPublisher;
  }

  // ===================== Public oqim =====================

  // MSISDN tekshirish: Turanix'ga imzolangan so'rov yuboradi, natijani saqlaydi,
  // log servisga yuboradi. Tashqi xato bo'lsa ham yozuv ERROR holatda saqlanadi.
  public Mono<TuranixCheckResponse> checkPassMsisdn(TuranixCheckRequest request) {
    log.info("Turanix check-pass-msisdn, msisdn={}", request.msisdn());
    CheckPassMsisdnRequest body =
        new CheckPassMsisdnRequest(
            request.msisdn(), request.pinfl(), request.passSer(), request.passNum());
    String bodyJson = toJson(body);

    return settingService
        .getCurrent()
        .flatMap(setting -> callTuranix(setting, bodyJson))
        .flatMap(response -> save(request, body, response, null))
        .onErrorResume(
            error -> {
              log.error("Turanix check-pass-msisdn xatosi", error);
              return save(request, body, null, error.getMessage());
            })
        .map(turanixMapper::toResponse);
  }

  // ===================== O'qish =====================

  public Mono<TuranixCheckResponse> getById(UUID id) {
    return checkRepository
        .findById(id)
        .switchIfEmpty(Mono.error(new NotFoundException("Turanix check not found")))
        .map(turanixMapper::toResponse);
  }

  public Flux<TuranixCheckResponse> getHistory(UUID userId, String msisdn, Pageable pageable) {
    if (userId != null) {
      return checkRepository
          .findAllByUserIdOrderByCreatedAtDesc(userId, pageable)
          .map(turanixMapper::toResponse);
    }
    if (msisdn != null && !msisdn.isBlank()) {
      return checkRepository
          .findAllByMsisdnOrderByCreatedAtDesc(msisdn, pageable)
          .map(turanixMapper::toResponse);
    }
    return checkRepository.findAllByOrderByCreatedAtDesc(pageable).map(turanixMapper::toResponse);
  }

  // ===================== Tashqi chaqiruv =====================

  // Imzolangan POST so'rov. bodyJson aynan shu baytlar imzolanadi va yuboriladi.
  private Mono<CheckPassMsisdnResponse> callTuranix(TuranixSettingEntity setting, String bodyJson) {
    if (setting.getSecretKey() == null || setting.getSecretKey().isBlank()) {
      return Mono.error(new BadRequestException("Turanix secret-key sozlanmagan"));
    }
    String timestamp = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();
    String nonce = UUID.randomUUID().toString();
    String signature =
        sign(setting.getSecretKey(), timestamp, setting.getProjectId(), bodyJson);

    WebClient client =
        webClientBuilder
            .baseUrl(setting.getBaseUrl())
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .build();

    return client
        .post()
        .uri(CHECK_PASS_MSISDN_PATH)
        .header("X-Timestamp", timestamp)
        .header("X-Nonce", nonce)
        .header("X-Project-Id", setting.getProjectId())
        .header("X-Signature", "sha256=" + signature)
        .bodyValue(bodyJson)
        .retrieve()
        .onStatus(HttpStatusCode::isError, this::turanixError)
        .bodyToMono(CheckPassMsisdnResponse.class);
  }

  // Canonical string: timestamp\nMETHOD\npath\nprojectId\n-\nsha256hex(body)
  // (Turanix HMAC-SHA256 talabi; "-" — partnyor so'rovi uchun X-ABR-ID o'rni).
  private String sign(String secret, String timestamp, String projectId, String bodyJson) {
    String canonical =
        String.join(
            "\n",
            timestamp,
            "POST",
            CHECK_PASS_MSISDN_PATH,
            projectId,
            "-",
            sha256Hex(bodyJson));
    return hmacSha256Hex(secret, canonical);
  }

  private String sha256Hex(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return toHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new BadRequestException("Turanix imzo: SHA-256 xatosi");
    }
  }

  private String hmacSha256Hex(String secret, String data) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return toHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new BadRequestException("Turanix imzo: HMAC xatosi");
    }
  }

  private String toHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) {
      sb.append(Character.forDigit((b >> 4) & 0xF, 16));
      sb.append(Character.forDigit(b & 0xF, 16));
    }
    return sb.toString();
  }

  // ===================== Saqlash + log =====================

  private Mono<TuranixCheckEntity> save(
      TuranixCheckRequest request,
      CheckPassMsisdnRequest body,
      CheckPassMsisdnResponse response,
      String errorMessage) {
    TuranixCheckEntity entity = new TuranixCheckEntity();
    entity.setUserId(request.userId());
    entity.setMsisdn(request.msisdn());
    entity.setPinfl(request.pinfl());
    entity.setPassSer(request.passSer());
    entity.setPassNum(request.passNum());
    entity.setStatus(errorMessage == null ? "SUCCESS" : "ERROR");
    entity.setErrorMessage(errorMessage);
    if (response != null) {
      entity.setResultCode(response.code());
      entity.setDescription(response.description());
    }
    entity.setRequest(toMap(body));
    entity.setResponse(toMap(response));
    entity.setCreatedAt(Instant.now());

    return checkRepository
        .save(entity)
        .doOnSuccess(saved -> sendTuranixLog(saved))
        .onErrorResume(
            e -> {
              // History saqlash o'xshamasa ham log yuboriladi, oqim to'xtamaydi.
              log.error("Turanix check saqlashda xato: {}", e.getMessage());
              entity.setId(null);
              sendTuranixLog(entity);
              return Mono.just(entity);
            });
  }

  // Turanix so'rovi logini log-servisga (RabbitMQ) yuboradi. Log yozish asosiy
  // oqimni to'xtatmasligi kerak — xato bo'lsa yutiladi.
  private void sendTuranixLog(TuranixCheckEntity entity) {
    jmsPublisher
        .publish(
            new TuranixLogReply(
                entity.getUserId(),
                entity.getMsisdn(),
                entity.getPinfl(),
                entity.getStatus(),
                entity.getErrorMessage(),
                toJson(entity.getRequest()),
                toJson(entity.getResponse()),
                Instant.now()))
        .subscribe();
  }

  // Turanix xato javobi: 403 bo'lsa Forbidden, aks holda BadRequest.
  private Mono<? extends Throwable> turanixError(ClientResponse clientResponse) {
    if (clientResponse.statusCode().value() == 403) {
      return Mono.error(new ForbiddenException("Turanix service'ga ruxsat yo'q (imzo noto'g'ri?)"));
    }
    return clientResponse
        .bodyToMono(String.class)
        .defaultIfEmpty("")
        .flatMap(
            errorBody -> {
              log.error("Turanix error {}: {}", clientResponse.statusCode(), errorBody);
              return Mono.error(
                  new BadRequestException(
                      "Turanix xatosi: " + clientResponse.statusCode().value()));
            });
  }

  // ===================== Helpers =====================

  // Obyektni JSON string'ga aylantiradi; xato bo'lsa null.
  private String toJson(Object value) {
    if (value == null) return null;
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      log.warn("Turanix JSON serialization failed: {}", e.getMessage());
      return null;
    }
  }

  // Obyektni Map<String,Object> ga aylantiradi (JSONB ustun uchun); xato bo'lsa null.
  private Map<String, Object> toMap(Object value) {
    if (value == null) return null;
    try {
      return objectMapper.convertValue(value, new TypeReference<>() {});
    } catch (IllegalArgumentException e) {
      log.warn("Turanix JSON convert failed: {}", e.getMessage());
      return null;
    }
  }
}
