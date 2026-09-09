package uz.hesap.service.document.webclient;

import java.util.List;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.*;
import uz.hesap.service.common.util.UserResponse;
import uz.hesap.service.document.context.WebClientConfig;

@Log4j2
@Service
public class UserServiceClient {
  private final WebClient webClient;

  @Autowired
  public UserServiceClient(
      final WebClient.Builder webClientBuilder,
      @Value("${application.main-service.base-url}") final String baseUrl) {
    this.webClient =
        webClientBuilder.baseUrl(baseUrl).filter(WebClientConfig.errorHandler()).build();
  }

  public Flux<UserBasicResponse> getUsersBasicInfo(List<UUID> userIds) {
    return webClient
        .post()
        .uri("/main/v1/local/users")
        .bodyValue(new UserIdsRequest(userIds))
        .retrieve()
        .bodyToFlux(UserBasicResponse.class);
  }

  public Flux<UserResponse> getUsersByIds(List<UUID> userIds) {
    return webClient
        .post()
        .uri("/main/v1/local/users")
        .bodyValue(new UserIdsRequest(userIds))
        .retrieve()
        .bodyToFlux(UserResponse.class);
  }

  // POST /local/users (id'lar bo'yicha to'liq UserResponse) — bitta id uchun.
  public Mono<UserResponse> getUserById(UUID userId) {
    return getUsersByIds(List.of(userId)).next();
  }

  // Shartnoma tuzilganda paket foydalanishini main'da qayd etadi (user_package_usage).
  // Xatolik asosiy create oqimini buzmasin — onErrorResume bilan yutib yuboramiz.
  public Mono<Void> recordPackageUsage(
      UUID userId, UUID userPackageId, UUID templateId, UUID contractId) {
    return webClient
        .post()
        .uri("/main/v1/local/package-usage")
        .bodyValue(new PackageUsageBody(userId, userPackageId, templateId, contractId))
        .retrieve()
        .bodyToMono(Void.class)
        .onErrorResume(
            e -> {
              log.warn("recordPackageUsage failed (contractId={}): {}", contractId, e.getMessage());
              return Mono.empty();
            });
  }

  public record PackageUsageBody(
      UUID userId, UUID userPackageId, UUID templateId, UUID contractId) {}

  // Alohida passport endpoint yo'q — to'liq UserResponse'dan yig'iladi.
  public Mono<UserPassportBasicResponse> getPartyPassportBasic(UUID userId) {
    return getUserById(userId).map(UserServiceClient::toPassportBasic);
  }

  // Kompaniyalar endi COMPANY typli userlar — POST /local/users'dan map qilinadi.
  public Flux<CompanyBasicResponse> getCompaniesByIds(List<UUID> companyIds) {
    return getUsersByIds(companyIds)
        .map(u -> new CompanyBasicResponse(u.id(), u.legalName(), null, u.tin()));
  }

  public Mono<UserResponse> getUserByIn(String in) {
    return webClient
        .get()
        .uri("/main/v1/local/users/{in}", in)
        .retrieve()
        .bodyToMono(UserResponse.class);
  }

  // PINFL/STIR (in) bo'yicha batch — har 'in' uchun UserBasicResponse, natija: in -> response.
  // Taraflar endi PINFL bilan aniqlanadi (buyer_in/seller_in/creator_in), UUID emas.
  public Mono<java.util.Map<String, UserBasicResponse>> getUsersBasicByIns(
      java.util.Collection<String> ins) {
    List<String> distinct =
        ins.stream().filter(s -> s != null && !s.isBlank()).distinct().toList();
    if (distinct.isEmpty()) return Mono.just(java.util.Map.of());
    return Flux.fromIterable(distinct)
        .flatMap(
            in ->
                getUserByIn(in)
                    .map(u -> java.util.Map.entry(in, toBasic(u)))
                    .onErrorResume(e -> Mono.empty()))
        .collectMap(java.util.Map.Entry::getKey, java.util.Map.Entry::getValue);
  }

  // PINFL/STIR (in) bo'yicha batch — har 'in' uchun to'liq UserResponse, natija: in -> response.
  // 404/xatolar o'tkazib yuboriladi (notification/enrichment flowni to'xtatmaslik uchun).
  public Mono<java.util.Map<String, UserResponse>> getUsersByIns(java.util.Collection<String> ins) {
    List<String> distinct =
        ins.stream().filter(s -> s != null && !s.isBlank()).distinct().toList();
    if (distinct.isEmpty()) return Mono.just(java.util.Map.of());
    return Flux.fromIterable(distinct)
        .flatMap(
            in ->
                getUserByIn(in)
                    .map(u -> java.util.Map.entry(in, u))
                    .onErrorResume(e -> Mono.empty()))
        .collectMap(java.util.Map.Entry::getKey, java.util.Map.Entry::getValue);
  }

  private static UserBasicResponse toBasic(UserResponse u) {
    return new UserBasicResponse(
        u.id(), u.firstName(), u.lastName(), u.phone(), u.legalName(), u.type());
  }

  // Fallback konvertor (main /passport-basic endpoint muvaffaqiyatsiz bo'lganda).
  // Direktor F.I.SH bu yerda mavjud emas (main /passport-basic'dan keladi) — bo'sh.
  private static UserPassportBasicResponse toPassportBasic(UserResponse u) {
    return new UserPassportBasicResponse(
        u.id(),
        u.fullName(),
        u.firstName(),
        u.lastName(),
        u.midName(),
        u.document(),
        u.in(),
        u.phone(),
        u.address(),
        u.legalName(),
        u.tin(),
        "",
        "",
        "",
        "",
        u.type(),
        u.verified());
  }
}
