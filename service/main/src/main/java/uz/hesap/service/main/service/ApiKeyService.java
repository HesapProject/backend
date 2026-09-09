package uz.hesap.service.main.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.exception.BadRequestException;
import uz.hesap.service.common.exception.ForbiddenException;
import uz.hesap.service.common.exception.NotFoundException;
import uz.hesap.service.common.util.ApiKeyContext;
import uz.hesap.service.common.util.ApiKeyResolveResponse;
import uz.hesap.service.common.util.UserPrincipal;
import uz.hesap.service.common.util.WebhookTargetResponse;
import uz.hesap.service.common.util.enums.ApiScope;
import uz.hesap.service.common.util.enums.UserType;
import uz.hesap.service.common.util.enums.WebhookEventType;
import uz.hesap.service.main.domain.ApiKeyEntity;
import uz.hesap.service.main.model.request.ApiKeyRequest;
import uz.hesap.service.main.model.response.ApiKeyCreatedResponse;
import uz.hesap.service.main.model.response.ApiKeyResponse;
import uz.hesap.service.main.repository.ApiKeyRepository;

/**
 * OpenAPI kalitlari. Kalit matni faqat yaratish/rotate javobida bir marta beriladi — DB'da
 * SHA-256 hash saqlanadi, shu sabab yo'qolgan kalitni tiklab bo'lmaydi (faqat rotate).
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class ApiKeyService {

  private static final String KEY_PREFIX = "hsp_";
  private static final int SECRET_LENGTH = 40;
  private static final int WEBHOOK_SECRET_LENGTH = 32;
  // Ko'rsatish uchun: hsp_ + dastlabki 6 belgi.
  private static final int VISIBLE_CHARS = 6;
  private static final String ALPHABET =
      "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

  private static final SecureRandom RANDOM = new SecureRandom();

  private final ApiKeyRepository apiKeyRepository;
  private final UserService userService;

  // ======================== KABINET (o'z kalitlari) ========================

  public Flux<ApiKeyResponse> getMine(final UserPrincipal principal) {
    return apiKeyRepository.findAllByOwner(requireOwnerIn(principal)).map(this::toResponse);
  }

  // Kalit yaratish. Kabinetdan kelganda egasi — joriy foydalanuvchi (PINFL/STIR);
  // admin tokenida request.ownerIn majburiy (kimga berilayotgani).
  public Mono<ApiKeyCreatedResponse> create(
      final UserPrincipal principal, final ApiKeyRequest request) {
    final String ownerIn = resolveOwnerIn(principal, request);
    validate(request);

    final String secret = randomString(SECRET_LENGTH);
    final String key = KEY_PREFIX + secret;

    ApiKeyEntity entity = new ApiKeyEntity();
    entity.setOwnerIn(ownerIn);
    entity.setOwnerUserId(principal.user() != null ? principal.user().id() : null);
    entity.setKeyPrefix(maskedKey(key));
    entity.setKeyHash(sha256(key));
    entity.setActive(Boolean.TRUE);
    applyRequest(entity, request);
    // Webhook secret — URL berilgan bo'lsa generatsiya qilinadi (HMAC imzosi uchun).
    if (entity.getWebhookUrl() != null) {
      entity.setWebhookSecret(randomString(WEBHOOK_SECRET_LENGTH));
    }

    return apiKeyRepository
        .save(entity)
        .map(saved -> new ApiKeyCreatedResponse(toResponse(saved), key, saved.getWebhookSecret()));
  }

  // Kalit sozlamalarini yangilash (kalit matni o'zgarmaydi).
  public Mono<ApiKeyResponse> update(
      final UserPrincipal principal, final UUID id, final ApiKeyRequest request) {
    validate(request);
    return findOwned(principal, id)
        .flatMap(
            entity -> {
              boolean hadWebhook = entity.getWebhookUrl() != null;
              applyRequest(entity, request);
              // URL birinchi marta qo'shilganda secret ham paydo bo'ladi.
              if (!hadWebhook && entity.getWebhookUrl() != null) {
                entity.setWebhookSecret(randomString(WEBHOOK_SECRET_LENGTH));
              }
              return apiKeyRepository.save(entity);
            })
        .map(this::toResponse);
  }

  // Kalitni bekor qilish — hash saqlanadi (audit), lekin resolve endi 401 beradi.
  public Mono<ApiKeyResponse> revoke(final UserPrincipal principal, final UUID id) {
    return findOwned(principal, id)
        .flatMap(
            entity -> {
              entity.setActive(Boolean.FALSE);
              entity.setRevokedAt(Instant.now());
              return apiKeyRepository.save(entity);
            })
        .map(this::toResponse);
  }

  // Yangi kalit matni — eskisi shu zahoti ishlamay qoladi.
  public Mono<ApiKeyCreatedResponse> rotate(final UserPrincipal principal, final UUID id) {
    final String key = KEY_PREFIX + randomString(SECRET_LENGTH);
    return findOwned(principal, id)
        .flatMap(
            entity -> {
              entity.setKeyPrefix(maskedKey(key));
              entity.setKeyHash(sha256(key));
              entity.setActive(Boolean.TRUE);
              entity.setRevokedAt(null);
              return apiKeyRepository.save(entity);
            })
        .map(saved -> new ApiKeyCreatedResponse(toResponse(saved), key, saved.getWebhookSecret()));
  }

  public Mono<Void> delete(final UserPrincipal principal, final UUID id) {
    return findOwned(principal, id)
        .flatMap(
            entity -> {
              entity.setDeleted(Boolean.TRUE);
              entity.setActive(Boolean.FALSE);
              return apiKeyRepository.save(entity);
            })
        .then();
  }

  // ======================== ADMIN (Control) ========================

  public Mono<Page<ApiKeyResponse>> getAllAdmin(final String search, final Pageable pageable) {
    final String filter = (search == null || search.isBlank()) ? null : search.trim();
    return apiKeyRepository
        .findPageAdmin(filter, pageable)
        .map(this::toResponse)
        .collectList()
        .zipWith(apiKeyRepository.countAllAdmin(filter))
        .map(tuple -> new PageImpl<>(tuple.getT1(), pageable, tuple.getT2()));
  }

  // ======================== INTERNAL (servislararo) ========================

  // X-API-Key qiymatini egasi + huquqlariga aylantiradi. Noto'g'ri/bekor qilingan/
  // muddati o'tgan kalit -> ForbiddenException (document servis 401 ga aylantiradi).
  public Mono<ApiKeyResolveResponse> resolve(final String rawKey) {
    if (rawKey == null || rawKey.isBlank()) {
      return Mono.error(new ForbiddenException("API kalit berilmagan"));
    }
    return apiKeyRepository
        .findByKeyHashAndDeletedFalse(sha256(rawKey.trim()))
        .switchIfEmpty(Mono.error(new ForbiddenException("API kalit topilmadi")))
        .flatMap(
            entity -> {
              if (!Boolean.TRUE.equals(entity.getActive())) {
                return Mono.error(new ForbiddenException("API kalit bekor qilingan"));
              }
              if (entity.getExpiresAt() != null
                  && !entity.getExpiresAt().isAfter(Instant.now())) {
                return Mono.error(new ForbiddenException("API kalit muddati tugagan"));
              }
              return userService
                  .getUserByIn(entity.getOwnerIn())
                  .switchIfEmpty(
                      Mono.error(new ForbiddenException("API kalit egasi topilmadi")))
                  .flatMap(
                      owner ->
                          // last_used_at — best-effort, xato oqimni to'xtatmaydi.
                          apiKeyRepository
                              .touchLastUsed(entity.getId())
                              .onErrorResume(e -> Mono.empty())
                              .thenReturn(new ApiKeyResolveResponse(owner, toContext(entity))));
            });
  }

  // Taraflarning (buyer/seller/creator) webhook manzillari — integration servis uchun.
  public Flux<WebhookTargetResponse> webhookTargets(final Collection<String> ins) {
    if (ins == null || ins.isEmpty()) {
      return Flux.empty();
    }
    return apiKeyRepository
        .findWebhookTargetsFor(ins.stream().filter(i -> i != null && !i.isBlank()).distinct().toList())
        .map(
            entity ->
                new WebhookTargetResponse(
                    entity.getId(),
                    entity.getOwnerIn(),
                    entity.getWebhookUrl(),
                    entity.getWebhookSecret(),
                    toEnums(entity.getWebhookEvents(), WebhookEventType::valueOf)));
  }

  // ======================== HELPERS ========================

  // Kalit egasi: admin bo'lsa request.ownerIn (majburiy), aks holda o'zi.
  private String resolveOwnerIn(final UserPrincipal principal, final ApiKeyRequest request) {
    if (isAdmin(principal)) {
      if (request.ownerIn() == null || request.ownerIn().isBlank()) {
        throw new BadRequestException("Kalit egasi (PINFL/STIR) ko'rsatilmagan");
      }
      return request.ownerIn().trim();
    }
    return requireOwnerIn(principal);
  }

  private String requireOwnerIn(final UserPrincipal principal) {
    final String in = principal.user() != null ? principal.user().identifier() : null;
    if (in == null || in.isBlank()) {
      throw new BadRequestException("Foydalanuvchi identifikatori (PINFL/STIR) aniqlanmadi");
    }
    return in;
  }

  private boolean isAdmin(final UserPrincipal principal) {
    UserType type = principal.user() != null ? principal.user().type() : null;
    return type == UserType.ADMIN || type == UserType.SUPER_ADMIN;
  }

  // Admin har qanday kalitni, oddiy foydalanuvchi faqat o'zinikini ko'radi/o'zgartiradi.
  private Mono<ApiKeyEntity> findOwned(final UserPrincipal principal, final UUID id) {
    return apiKeyRepository
        .findByIdAndDeletedFalse(id)
        .switchIfEmpty(Mono.error(new NotFoundException("API kalit topilmadi")))
        .flatMap(
            entity -> {
              if (isAdmin(principal)) {
                return Mono.just(entity);
              }
              return entity.getOwnerIn().equals(requireOwnerIn(principal))
                  ? Mono.just(entity)
                  : Mono.error(new ForbiddenException("Bu kalit sizga tegishli emas"));
            });
  }

  private void validate(final ApiKeyRequest request) {
    if (request.name() == null || request.name().isBlank()) {
      throw new BadRequestException("Kalit nomi bo'sh bo'lmasin");
    }
    if (request.scopes() == null || request.scopes().isEmpty()) {
      throw new BadRequestException("Kamida bitta huquq (scope) tanlanishi kerak");
    }
    // Full emas -> shablonlar ro'yxati majburiy (aks holda kalit hech nima qila olmaydi).
    if (!Boolean.TRUE.equals(request.allTemplates())
        && (request.templateIds() == null || request.templateIds().isEmpty())) {
      throw new BadRequestException("Shablon tanlang yoki barcha shablonlarga ruxsat bering");
    }
    if (request.expiresAt() != null && !request.expiresAt().isAfter(Instant.now())) {
      throw new BadRequestException("Amal qilish muddati kelajakdagi sana bo'lishi kerak");
    }
    if (request.webhookUrl() != null
        && !request.webhookUrl().isBlank()
        && !request.webhookUrl().startsWith("https://")
        && !request.webhookUrl().startsWith("http://")) {
      throw new BadRequestException("Webhook URL http(s):// bilan boshlanishi kerak");
    }
  }

  private void applyRequest(final ApiKeyEntity entity, final ApiKeyRequest request) {
    entity.setName(request.name().trim());
    entity.setScopes(toNames(request.scopes()));
    entity.setAllTemplates(Boolean.TRUE.equals(request.allTemplates()));
    // Full bo'lsa ro'yxat saqlanmaydi — keyin full o'chirilsa eski ro'yxat qayta tirilmasin.
    entity.setTemplateIds(
        Boolean.TRUE.equals(request.allTemplates())
            ? null
            : request.templateIds().toArray(new UUID[0]));
    entity.setExpiresAt(request.expiresAt());
    entity.setWebhookUrl(
        (request.webhookUrl() == null || request.webhookUrl().isBlank())
            ? null
            : request.webhookUrl().trim());
    entity.setWebhookEvents(toNames(request.webhookEvents()));
  }

  private ApiKeyResponse toResponse(final ApiKeyEntity entity) {
    boolean expired =
        entity.getExpiresAt() != null && !entity.getExpiresAt().isAfter(Instant.now());
    return new ApiKeyResponse(
        entity.getId(),
        entity.getName(),
        entity.getOwnerIn(),
        entity.getKeyPrefix(),
        toEnums(entity.getScopes(), ApiScope::valueOf),
        entity.getAllTemplates(),
        entity.getTemplateIds() == null ? List.of() : Arrays.asList(entity.getTemplateIds()),
        entity.getExpiresAt(),
        expired,
        entity.getWebhookUrl(),
        entity.getWebhookSecret() != null,
        toEnums(entity.getWebhookEvents(), WebhookEventType::valueOf),
        entity.getActive(),
        entity.getRevokedAt(),
        entity.getLastUsedAt(),
        entity.getCreatedDate());
  }

  private ApiKeyContext toContext(final ApiKeyEntity entity) {
    return new ApiKeyContext(
        entity.getId(),
        entity.getName(),
        entity.getOwnerIn(),
        toEnums(entity.getScopes(), ApiScope::valueOf),
        entity.getAllTemplates(),
        entity.getTemplateIds() == null ? List.of() : Arrays.asList(entity.getTemplateIds()),
        entity.getExpiresAt());
  }

  private static <E extends Enum<E>> List<E> toEnums(
      final String[] names, final java.util.function.Function<String, E> parser) {
    if (names == null) {
      return List.of();
    }
    return Arrays.stream(names)
        .map(
            name -> {
              try {
                return parser.apply(name);
              } catch (IllegalArgumentException e) {
                // Enum'dan olib tashlangan eski qiymat — o'tkazib yuboramiz.
                log.warn("Noma'lum enum qiymati e'tiborsiz qoldirildi: {}", name);
                return null;
              }
            })
        .filter(java.util.Objects::nonNull)
        .toList();
  }

  private static <E extends Enum<E>> String[] toNames(final List<E> values) {
    if (values == null) {
      return new String[0];
    }
    return values.stream().map(Enum::name).distinct().toArray(String[]::new);
  }

  // hsp_ab12cd•••• — UI'da kalitni tanish uchun.
  private static String maskedKey(final String key) {
    return key.substring(0, KEY_PREFIX.length() + VISIBLE_CHARS) + "••••••••";
  }

  private static String randomString(final int length) {
    StringBuilder sb = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
    }
    return sb.toString();
  }

  private static String sha256(final String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 mavjud emas", e);
    }
  }
}
