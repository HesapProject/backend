package uz.hesap.service.integration.service;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uz.hesap.service.integration.domain.EImzoSettingEntity;
import uz.hesap.service.integration.model.eimzo.EImzoAuthResponse;
import uz.hesap.service.integration.model.eimzo.EImzoVerifyResponse;

// E-IMZO server bilan barcha HTTP aloqa shu yerda — auth, verify-attached, timestamp.
// Base URL har request oldidan DB'dan o'qiladi (admin UI'da o'zgartirilgani darhol qo'llaniladi).
@Log4j2
@Service
public class EImzoHttpService {

  private final EImzoSettingService settingService;
  private final WebClient webClient;

  public EImzoHttpService(EImzoSettingService settingService, WebClient.Builder webClientBuilder) {
    this.settingService = settingService;
    this.webClient = webClientBuilder.build();
  }

  // /backend/auth — sertifikat ma'lumotini qaytaradi (PINFL/CN/...).
  public Mono<EImzoAuthResponse> auth(String pkcs7, String ipAddress) {
    return settingService
        .getCurrent()
        .flatMap(
            c ->
                webClient
                    .post()
                    .uri(baseUrl(c) + "/backend/auth")
                    .header("Content-Type", "text/plain")
                    .header("X-Real-IP", safeIp(ipAddress))
                    .bodyValue(pkcs7)
                    .retrieve()
                    .bodyToMono(EImzoAuthResponse.class))
        .doOnError(e -> log.error("E-IMZO auth failed: {}", e.getMessage()));
  }

  // /backend/pkcs7/verify/attached — imzoni tekshirish (hujjat saqlash uchun).
  public Mono<EImzoVerifyResponse> verifyAttached(String pkcs7, String ipAddress, String host) {
    return settingService
        .getCurrent()
        .flatMap(
            c ->
                webClient
                    .post()
                    .uri(baseUrl(c) + "/backend/pkcs7/verify/attached")
                    .header("Content-Type", "text/plain")
                    .header("X-Real-IP", safeIp(ipAddress))
                    .header("Host", safeHost(host))
                    .bodyValue(pkcs7)
                    .retrieve()
                    .bodyToMono(EImzoVerifyResponse.class))
        .doOnError(e -> log.error("E-IMZO verify failed: {}", e.getMessage()));
  }

  // /frontend/timestamp/pkcs7 — TSA timestamp olish.
  public Mono<String> getTimestamp(String pkcs7, String ipAddress, String host) {
    return settingService
        .getCurrent()
        .flatMap(
            c ->
                webClient
                    .post()
                    .uri(baseUrl(c) + "/frontend/timestamp/pkcs7")
                    .header("Content-Type", "text/plain")
                    .header("X-Real-IP", safeIp(ipAddress))
                    .header("Host", safeHost(host))
                    .bodyValue(pkcs7)
                    .retrieve()
                    .bodyToMono(String.class))
        .doOnError(e -> log.error("E-IMZO timestamp failed: {}", e.getMessage()));
  }

  private String baseUrl(EImzoSettingEntity c) {
    return c.getBaseUrl();
  }

  private String safeIp(String ip) {
    return (ip == null || ip.isBlank()) ? "127.0.0.1" : ip;
  }

  private String safeHost(String host) {
    return (host == null || host.isBlank()) ? "hesap.uz" : host;
  }
}
