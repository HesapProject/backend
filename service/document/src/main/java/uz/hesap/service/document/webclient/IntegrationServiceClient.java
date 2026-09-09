package uz.hesap.service.document.webclient;

import java.util.UUID;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uz.hesap.service.document.context.WebClientConfig;
import uz.hesap.service.document.model.response.eimzo.EImzoVerifyResponse;

// E-IMZO server bilan barcha HTTP integration servisida. document-service
// faqat hujjat orchestratori — Feign'dan tashqari HTTP yo'q.
@Log4j2
@Service
public class IntegrationServiceClient {

  // E-IMZO verify-attached/timestamp so'rovi.
  public record EImzoVerifyRequest(String pkcs7, String ipAddress, String host) {}

  // MyID: code → imzolovchini tekshirish (s2s). platform: "WEB"/"MOBILE".
  public record MyIdVerifyRequest(String code, UUID userId, String platform) {}

  // MyID natijasining minimal qismi: pinfl + yuz mosligi bali.
  public record MyIdVerifyResponse(String pinfl, Double comparisonValue) {}

  private final WebClient webClient;

  public IntegrationServiceClient(
      WebClient.Builder webClientBuilder,
      @Value("${application.integration-service.base-url:}") String baseUrl) {
    this.webClient =
        webClientBuilder.baseUrl(baseUrl).filter(WebClientConfig.errorHandler()).build();
  }

  public Mono<EImzoVerifyResponse> verifyAttached(String pkcs7, String ipAddress, String host) {
    return webClient
        .post()
        .uri("/integration/v1/local/eimzo/verify-attached")
        .bodyValue(new EImzoVerifyRequest(pkcs7, ipAddress, host))
        .retrieve()
        .bodyToMono(EImzoVerifyResponse.class);
  }

  public Mono<String> getTimestamp(String pkcs7, String ipAddress, String host) {
    return webClient
        .post()
        .uri("/integration/v1/local/eimzo/timestamp")
        .bodyValue(new EImzoVerifyRequest(pkcs7, ipAddress, host))
        .retrieve()
        .bodyToMono(String.class);
  }

  // MyID code'ni tekshirib imzolovchi pinfl + yuz mosligi balini qaytaradi.
  public Mono<MyIdVerifyResponse> verifyMyId(String code, UUID userId, String platform) {
    return webClient
        .post()
        .uri("/integration/v1/local/myid/verify")
        .bodyValue(new MyIdVerifyRequest(code, userId, platform))
        .retrieve()
        .bodyToMono(MyIdVerifyResponse.class);
  }

  // AbleID attempt: sessiya kimga ochilgan (pinfl) + status (PENDING/SUCCESS).
  public record AbleIdVerifyRequest(String attemptId) {}

  public record AbleIdVerifyResponse(String pinfl, String status) {}

  public Mono<AbleIdVerifyResponse> verifyAbleId(String attemptId) {
    return webClient
        .post()
        .uri("/integration/v1/local/able-id/verify")
        .bodyValue(new AbleIdVerifyRequest(attemptId))
        .retrieve()
        .bodyToMono(AbleIdVerifyResponse.class);
  }

  // ============ Rouming (Factura Provider — ЭСФ) ============

  // Draft ЭСФ so'rovi — summalar SO'MDA, sanalar yyyy-MM-dd.
  public record RoumingProductLine(String name, Double count, Double unitPrice, Double totalSum) {}

  public record RoumingFacturaDraftRequest(
      UUID contractId,
      String sellerTin,
      String buyerTin,
      String sellerName,
      String buyerName,
      String facturaNo,
      String facturaDate,
      String contractNo,
      String contractDate,
      java.util.List<RoumingProductLine> products) {}

  // Oldi-berdi tasdiqlanganda Rouming'da draft schyot-faktura yaratish.
  // Xato asosiy oqimni buzmasin — chaqiruvchi onErrorResume bilan qamraydi.
  public Mono<String> createRoumingFacturaDraft(RoumingFacturaDraftRequest request) {
    return webClient
        .post()
        .uri("/integration/v1/local/rouming/factura-draft")
        .bodyValue(request)
        .retrieve()
        .bodyToMono(String.class);
  }
}
