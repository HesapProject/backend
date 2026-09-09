package uz.hesap.service.integration.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.exception.BadRequestException;
import uz.hesap.service.common.exception.NotFoundException;
import uz.hesap.service.integration.domain.AbleIdAttemptEntity;
import uz.hesap.service.integration.domain.AbleIdSettingEntity;
import uz.hesap.service.integration.model.ableid.AbleIdSessionResponse;
import uz.hesap.service.integration.model.ableid.AbleIdVerifyResponse;
import uz.hesap.service.integration.repository.AbleIdAttemptRepository;

// AbleID (theable.tech) identifikatsiya sessiyalari:
//  1) createSession — {baseUrl}/check/start/second (rezident: pinfl + birthDate),
//     javob: attemptId + fullUrl. SDK'ga attemptId + domen kerak.
//  2) handleHook — webhook FAQAT muvaffaqiyatda keladi; hash tekshiriladi:
//     sha1(sha1(projectId+secret).toUpperCase() + attemptId).toUpperCase()
//  3) verify — s2s: attempt statusi + kimga ochilgani (imzo/profil tasdiqlash uchun).
@Log4j2
@Service
@RequiredArgsConstructor
public class AbleIdService {

  public static final String STATUS_PENDING = "PENDING";
  public static final String STATUS_SUCCESS = "SUCCESS";

  private final AbleIdSettingService settingService;
  private final AbleIdAttemptRepository attemptRepository;
  private final WebClient.Builder webClientBuilder;
  private final ObjectMapper objectMapper;

  // Foydalanuvchi (PINFL) uchun yangi sessiya ochadi.
  public Mono<AbleIdSessionResponse> createSession(String userIn, String lang) {
    if (userIn == null || userIn.isBlank()) {
      return Mono.error(new BadRequestException("Foydalanuvchi PINFL topilmadi"));
    }
    String birthDate = birthDateFromPinfl(userIn);
    if (birthDate == null) {
      return Mono.error(new BadRequestException("PINFL'dan tug'ilgan sana aniqlanmadi"));
    }
    return settingService
        .getCurrent()
        .flatMap(
            setting -> {
              if (setting.getProjectId() == null || setting.getProjectId().isBlank()
                  || setting.getSecret() == null || setting.getSecret().isBlank()) {
                return Mono.error(
                    new BadRequestException(
                        "AbleID sozlanmagan — Control'dagi Integratsiyalar bo'limida"
                            + " projectId/secret kiriting"));
              }
              String transactionId = UUID.randomUUID().toString();
              Map<String, Object> body =
                  Map.of(
                      "projectId", setting.getProjectId(),
                      "secret", setting.getSecret(),
                      "transactionId", transactionId,
                      "pinfl", userIn,
                      "birthDate", birthDate,
                      "lang", normalizeLang(lang),
                      "hooks", List.of(hookUrl(setting)));
              return webClientBuilder
                  .baseUrl(setting.getBaseUrl())
                  .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                  .build()
                  .post()
                  .uri("/check/start/second")
                  .bodyValue(body)
                  .retrieve()
                  .bodyToMono(JsonNode.class)
                  .flatMap(json -> saveAttempt(json, transactionId, userIn, setting));
            });
  }

  private Mono<AbleIdSessionResponse> saveAttempt(
      JsonNode json, String transactionId, String userIn, AbleIdSettingEntity setting) {
    JsonNode data = json.path("data");
    String attemptId = data.path("attemptId").asText(null);
    String fullUrl = data.path("fullUrl").asText(null);
    if (attemptId == null || attemptId.isBlank()) {
      log.error("AbleID sessiya yaratilmadi, javob: {}", json);
      String msg = json.path("message").asText("AbleID sessiya yaratib bo'lmadi");
      return Mono.error(new BadRequestException(msg));
    }
    AbleIdAttemptEntity attempt = new AbleIdAttemptEntity();
    attempt.setAttemptId(attemptId);
    attempt.setTransactionId(transactionId);
    attempt.setUserIn(userIn);
    attempt.setStatus(STATUS_PENDING);
    return attemptRepository
        .save(attempt)
        .thenReturn(new AbleIdSessionResponse(attemptId, setting.getBaseUrl(), fullUrl));
  }

  // Webhook (public) — hash to'g'ri bo'lsa attempt SUCCESS qilinadi.
  public Mono<Void> handleHook(JsonNode payload) {
    JsonNode data = payload.path("data");
    String attemptId = data.path("attemptId").asText(null);
    String hash = data.path("hash").asText(null);
    if (attemptId == null || attemptId.isBlank()) {
      log.warn("AbleID hook attemptId'siz keldi: {}", payload);
      return Mono.empty();
    }
    return settingService
        .getCurrent()
        .flatMap(
            setting -> {
              String expected = expectedHash(setting, attemptId);
              if (hash == null || !expected.equalsIgnoreCase(hash)) {
                log.error("AbleID hook hash mos emas, attemptId={}", attemptId);
                return Mono.error(new BadRequestException("AbleID hash mos emas"));
              }
              return attemptRepository
                  .findByAttemptId(attemptId)
                  .switchIfEmpty(
                      Mono.error(new NotFoundException("AbleID attempt topilmadi: " + attemptId)))
                  .flatMap(
                      attempt -> {
                        attempt.setStatus(STATUS_SUCCESS);
                        attempt.setPayload(toJson(data));
                        return attemptRepository.save(attempt);
                      })
                  .then();
            });
  }

  // s2s / klient: attempt holati.
  public Mono<AbleIdVerifyResponse> verify(String attemptId) {
    return attemptRepository
        .findByAttemptId(attemptId)
        .switchIfEmpty(Mono.error(new NotFoundException("AbleID attempt topilmadi")))
        .map(a -> new AbleIdVerifyResponse(a.getUserIn(), a.getStatus()));
  }

  // hash = sha1(sha1(projectId+secret).toUpperCase() + attemptId).toUpperCase()
  private static String expectedHash(AbleIdSettingEntity setting, String attemptId) {
    String inner = sha1Hex(setting.getProjectId() + setting.getSecret()).toUpperCase();
    return sha1Hex(inner + attemptId).toUpperCase();
  }

  private static String sha1Hex(String value) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-1");
      return HexFormat.of().formatHex(md.digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException("SHA-1 mavjud emas", e);
    }
  }

  // PINFL: 1-raqam asr (1/2→18xx, 3/4→19xx, 5/6→20xx), 2-7 raqamlar DDMMYY.
  // AbleID DD.MM.YYYY formatini kutadi.
  static String birthDateFromPinfl(String pinfl) {
    if (pinfl == null || !pinfl.matches("^[1-6][0-9]{13}$")) return null;
    char c = pinfl.charAt(0);
    String century = (c == '1' || c == '2') ? "18" : (c == '3' || c == '4') ? "19" : "20";
    String dd = pinfl.substring(1, 3);
    String mm = pinfl.substring(3, 5);
    String yy = pinfl.substring(5, 7);
    return dd + "." + mm + "." + century + yy;
  }

  private String hookUrl(AbleIdSettingEntity setting) {
    return setting.getHookUrl() == null || setting.getHookUrl().isBlank()
        ? "https://api.business.hesap.uz/integration/v1/able-id/hook"
        : setting.getHookUrl();
  }

  private static String normalizeLang(String lang) {
    String l = lang == null ? "uz" : lang.toLowerCase();
    return switch (l) {
      case "ru", "en", "uz", "oz" -> l;
      default -> "uz";
    };
  }

  private String toJson(Object o) {
    try {
      return objectMapper.writeValueAsString(o);
    } catch (Exception e) {
      return String.valueOf(o);
    }
  }
}
