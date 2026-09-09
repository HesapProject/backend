package uz.hesap.service.integration.service.scoring;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
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
import uz.hesap.service.integration.domain.*;
import uz.hesap.service.integration.domain.enums.BalanceType;
import uz.hesap.service.integration.domain.enums.BillingType;
import uz.hesap.service.integration.domain.enums.CardType;
import uz.hesap.service.integration.domain.enums.ScoringStatus;
import uz.hesap.service.integration.domain.enums.ScoringType;
import uz.hesap.service.integration.domain.enums.TransactionType;
import uz.hesap.service.integration.model.CardResponse;
import uz.hesap.service.integration.model.mapper.PlumMapper;
import uz.hesap.service.integration.model.plum.*;
import uz.hesap.service.integration.model.uzcard.*;
import uz.hesap.service.integration.repository.BalanceRepository;
import uz.hesap.service.integration.repository.PlumScoringRepository;
import uz.hesap.service.integration.repository.TransactionRepository;
import uz.hesap.service.integration.repository.UserCardRepository;
import uz.hesap.service.integration.service.PlumSettingService;
import uz.hesap.service.integration.service.payment.BalanceHelper;
import uz.hesap.service.integration.service.payment.PaymentRecorder;
import uz.hesap.service.common.exception.BadRequestException;
import uz.hesap.service.common.exception.ForbiddenException;
import uz.hesap.service.common.exception.NotFoundException;
import uz.hesap.service.common.util.enums.PaymentMethod;
import uz.hesap.service.common.util.message.PlumLogReply;
import uz.hesap.service.jms.JmsPublisher;

@Log4j2
@Service
public class PlumService {

  private final WebClient.Builder webClientBuilder;
  private final PlumSettingService plumSettingService;
  private final PlumScoringRepository scoringRepository;
  private final UserCardRepository userCardRepository;
  private final PlumMapper plumMapper;
  private final ObjectMapper objectMapper;
  private final JmsPublisher jmsPublisher;
  private final BalanceHelper balanceHelper;
  private final PaymentRecorder paymentRecorder;
  private final TransactionRepository transactionRepository;
  private final BalanceRepository balanceRepository;
  // user UUID → PINFL resolish uchun (scoring user_in to'ldirish).
  private final uz.hesap.service.integration.webclient.UserServiceClient webUserServiceClient;

  public PlumService(
      WebClient.Builder webClientBuilder,
      PlumSettingService plumSettingService,
      PlumScoringRepository scoringRepository,
      UserCardRepository userCardRepository,
      PlumMapper plumMapper,
      ObjectMapper objectMapper,
      JmsPublisher jmsPublisher,
      BalanceHelper balanceHelper,
      PaymentRecorder paymentRecorder,
      TransactionRepository transactionRepository,
      BalanceRepository balanceRepository,
      uz.hesap.service.integration.webclient.UserServiceClient webUserServiceClient) {
    this.webClientBuilder = webClientBuilder;
    this.plumSettingService = plumSettingService;
    this.scoringRepository = scoringRepository;
    this.userCardRepository = userCardRepository;
    this.plumMapper = plumMapper;
    this.objectMapper = objectMapper;
    this.jmsPublisher = jmsPublisher;
    this.balanceHelper = balanceHelper;
    this.paymentRecorder = paymentRecorder;
    this.transactionRepository = transactionRepository;
    this.balanceRepository = balanceRepository;
    this.webUserServiceClient = webUserServiceClient;
  }

  // PlumSettingService'dan aktual creds olib, har so'rovda yangi WebClient quradi.
  private Mono<WebClient> plumWebClient() {
    return plumSettingService
        .getCurrent()
        .handle(
            (setting, sink) -> {
              if (setting.getBaseUrl() == null
                  || setting.getLogin() == null
                  || setting.getPassword() == null) {
                sink.error(
                    new BadRequestException(
                        "Plum credentials not configured. "
                            + "POST /integration/v1/plum/settings orqali yarating."));
                return;
              }
              String authHeader = basicAuthHeader(setting.getLogin(), setting.getPassword());
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

  // Basic auth header: "Basic base64(login:password)"
  private String basicAuthHeader(String login, String password) {
    String auth = login + ":" + password;
    return "Basic " + Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
  }

  // ===================== UserCard (karta qo'shish/boshqarish) =====================
  // Billing'dan ko'chirildi — endi creds DB'dan (PlumSettingService) olinadi.

  // Karta yaratishni boshlaydi — Plum OTP yuboradi, session qaytaradi.
  public Mono<CreateUserCardResponse> createUserCard(CreateUserCardRequest request) {
    log.info("Creating user card for pinfl: {}", request.pinfl());
    // Plum expireDate'ni YYMM formatida kutadi; frontend MMYY yuboradi.
    // userId maydoniga PINFL yuboramiz — confirm'da echo bo'lib, user_in (PINFL) saqlanadi.
    PlumCreateCardBody plumRequest =
        new PlumCreateCardBody(
            request.pinfl(),
            request.cardNumber(),
            toYyMm(request.expireDate()),
            request.userPhone(),
            request.pinfl());
    String requestBody = toJson(plumRequest);
    return plumWebClient()
        .flatMap(
            client ->
                client
                    .post()
                    .uri("/UserCard/createUserCard")
                    .bodyValue(plumRequest)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, this::plumError)
                    .bodyToMono(CreateUserCardResponse.class))
        .switchIfEmpty(Mono.error(new BadRequestException("Plum service'dan bo'sh javob keldi")))
        .flatMap(
            response -> {
              if (response.error() != null) {
                log.error("Error creating user card: {}", response.error());
                return Mono.error(new BadRequestException(friendlyPlumMessage(response.error())));
              }
              log.info("User card creation initiated, session: {}", response.result().session());
              return Mono.just(response);
            })
        .doOnSuccess(
            response ->
                sendPlumLog(
                    request.userId(),
                    null,
                    "CARD_CREATE",
                    "SUCCESS",
                    null,
                    requestBody,
                    toJson(response)))
        .doOnError(
            error -> {
              log.error("Error creating user card request", error);
              sendPlumLog(
                  request.userId(),
                  null,
                  "CARD_CREATE",
                  "ERROR",
                  error.getMessage(),
                  requestBody,
                  null);
            });
  }

  // OTP tasdiqlaydi va kartani billing.plum_cards'ga saqlaydi.
  public Mono<ConfirmUserCardResponse> confirmUserCard(ConfirmUserCardRequest request) {
    log.info("Confirming user card for session: {}", request.session());
    var providerRequest = new ProviderConfirmRequest(request.session(), request.otp(), 0);
    String requestBody = toJson(providerRequest);
    return plumWebClient()
        .flatMap(
            client ->
                client
                    .post()
                    .uri("/UserCard/confirmUserCardCreate")
                    .bodyValue(providerRequest)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, this::plumError)
                    .bodyToMono(ConfirmUserCardResponse.class))
        .switchIfEmpty(Mono.error(new BadRequestException("Plum service'dan bo'sh javob keldi")))
        .flatMap(
            response -> {
              if (response.error() != null) {
                log.error("Error confirming user card: {}", response.error());
                return Mono.error(new BadRequestException(response.error().errorMessage()));
              }
              log.info("User card confirmed, cardId: {}", response.result().card().id());
              // Plum userId maydonida biz yuborgan PINFL'ni echo qiladi → user_in.
              String userIn = response.result().card().userId();
              sendPlumLog(
                  null, null, "CARD_CONFIRM", "SUCCESS", null, requestBody, toJson(response));
              CardType type = CardType.detect(response.result().card().number());
              UserCardEntity entity =
                  UserCardEntity.builder()
                      .type(type)
                      .userIn(userIn)
                      .cardNumber(response.result().card().number())
                      .expireDate(toYySlashMm(response.result().card().expireDate()))
                      .session(request.session())
                      .cardId(response.result().card().cardId())
                      .status(response.result().card().status())
                      .build();
              // Karta faqat integration'da (billing.plum_cards) — main'ga push qilinmaydi.
              return userCardRepository.save(entity).thenReturn(response);
            })
        .doOnError(
            error -> {
              log.error("Error confirming user card request", error);
              sendPlumLog(
                  null, null, "CARD_CONFIRM", "ERROR", error.getMessage(), requestBody, null);
            });
  }

  // MMYY -> YYMM (frontend MMYY yuboradi, Plum YYMM kutadi). 0130 -> 3001
  private String toYyMm(String mmYy) {
    if (mmYy == null || mmYy.length() != 4) {
      return mmYy;
    }
    return mmYy.substring(2) + mmYy.substring(0, 2);
  }

  // 4-raqamli muddatni YY/MM ga keltiradi (integration billing.plum_cards uchun).
  private String toYySlashMm(String raw) {
    if (raw == null) {
      return null;
    }
    String d = raw.replaceAll("\\D", "");
    if (d.length() != 4) {
      return raw;
    }
    String first = d.substring(0, 2);
    String second = d.substring(2);
    if (Integer.parseInt(first) > 12) {
      return first + "/" + second; // YYMM -> YY/MM
    }
    return second + "/" + first; // MMYY -> YY/MM
  }

  // OTP'ni qayta yuboradi.
  public Mono<ResendOtpResponse> resendOtp(Integer session) {
    log.info("Resending OTP for session: {}", session);
    String requestBody = "session=" + session;
    return plumWebClient()
        .flatMap(
            client ->
                client
                    .get()
                    .uri(
                        uriBuilder ->
                            uriBuilder
                                .path("/UserCard/resendOtp")
                                .queryParam("session", session)
                                .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, this::plumError)
                    .bodyToMono(ResendOtpResponse.class))
        .switchIfEmpty(Mono.error(new BadRequestException("Plum service'dan bo'sh javob keldi")))
        .doOnSuccess(
            response -> {
              if (response.error() != null) {
                log.error("Error resending OTP: {}", response.error());
              } else {
                log.info("OTP resent, new session: {}", response.result().session());
              }
              sendPlumLog(
                  null, null, "CARD_RESEND_OTP", "SUCCESS", null, requestBody, toJson(response));
            })
        .doOnError(
            error -> {
              log.error("Error resending OTP request", error);
              sendPlumLog(
                  null, null, "CARD_RESEND_OTP", "ERROR", error.getMessage(), requestBody, null);
            });
  }

  // Kartani Plum'dan va billing.plum_cards'dan o'chiradi.
  public Mono<DeleteUserCardResponse> deleteUserCard(UUID userCardId) {
    log.info("Deleting user card with id: {}", userCardId);
    // DIQQAT: Plum deleteUserCard `userCardId` sifatida ro'yxatdagi `id`ni kutadi
    // (bizda saqlanadigan `cardId` EMAS — u bilan «Карта не найдена» qaytaradi).
    // Shuning uchun avval Plum ro'yxatidan mos kartaning `id`sini topamiz.
    return userCardRepository
        .findById(userCardId)
        .switchIfEmpty(
            Mono.error(new NotFoundException("Card not found in database with id: " + userCardId)))
        .flatMap(
            userCardEntity ->
                getUserCardsFromPlum(userCardEntity.getUserIn())
                    .filter(
                        pc ->
                            (pc.cardId() != null && pc.cardId().equals(userCardEntity.getCardId()))
                                || panMatches(pc.number(), userCardEntity.getCardNumber()))
                    .next()
                    .flatMap(
                        match ->
                            match.userCardId() == null
                                ? Mono.error(
                                    new BadRequestException(
                                        "Plum ro'yxatida karta id'si topilmadi"))
                                : deleteFromPlum(match.userCardId())
                                    .flatMap(
                                        response -> {
                                          log.info("Plum deleted successfully, now deleting from DB");
                                          sendPlumLog(
                                              null,
                                              userCardEntity.getId(),
                                              "CARD_DELETE",
                                              "SUCCESS",
                                              null,
                                              "plumUserCardId=" + match.userCardId(),
                                              toJson(response));
                                          return userCardRepository
                                              .delete(userCardEntity)
                                              .thenReturn(response);
                                        }))
                    // Plum ro'yxatida yo'q — u yoqda allaqachon o'chirilgan; lokalni tozalaymiz.
                    .switchIfEmpty(
                        Mono.defer(
                            () -> {
                              log.info("Card not present in Plum, deleting local row only");
                              sendPlumLog(
                                  null,
                                  userCardEntity.getId(),
                                  "CARD_DELETE",
                                  "SUCCESS",
                                  null,
                                  "faqat lokal (Plum'da yo'q), cardId="
                                      + userCardEntity.getCardId(),
                                  null);
                              return userCardRepository
                                  .delete(userCardEntity)
                                  .thenReturn(
                                      new DeleteUserCardResponse(
                                          new DeleteUserCardResponse.Result(Boolean.TRUE), null));
                            })))
        .doOnError(
            error -> {
              log.error("Error in deleteUserCard process", error);
              sendPlumLog(
                  null, null, "CARD_DELETE", "ERROR", error.getMessage(), null, null);
            });
  }

  // Plum'dan userCard `id` (ro'yxatdagi id) bilan o'chiradi; Plum error body
  // qaytarsa xato sifatida ko'taramiz (klient 200 deb adashmasin).
  private Mono<DeleteUserCardResponse> deleteFromPlum(Long plumUserCardId) {
    return plumWebClient()
        .flatMap(
            client ->
                client
                    .delete()
                    .uri(
                        uriBuilder ->
                            uriBuilder
                                .path("/UserCard/deleteUserCard")
                                .queryParam("userCardId", plumUserCardId)
                                .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, this::plumError)
                    .bodyToMono(DeleteUserCardResponse.class))
        .flatMap(
            response ->
                response.error() != null
                    // DeleteUserCardResponse.error tipsiz (Object) — matnini ko'taramiz.
                    ? Mono.error(
                        new BadRequestException(String.valueOf(response.error())))
                    : Mono.just(response));
  }

  // Foydalanuvchi kartalari ro'yxati PINFL/STIR (user_in) bo'yicha — billing.plum_cards.
  // Kartalar manbai = PLUM (getAllUserCards). Har GET'da Plum'dan olib, lokal DB'ni
  // moslaymiz (Plum'da yo'q yetimlarni o'chiramiz, Plum'da bor yangilarni qo'shamiz),
  // so'ng UUID'li lokal ro'yxatni qaytaramiz (delete/scoring UUID bilan ishlaydi).
  // Plum vaqtincha ishlamasa — lokal DB'dan qaytaramiz (GET buzilmasin).
  public Flux<CardResponse> getUserCards(String userIn) {
    log.debug("Find all user card {} (Plum manba)", userIn);
    return getUserCardsFromPlum(userIn)
        .collectList()
        .flatMapMany(plum -> reconcileCards(userIn, plum))
        .onErrorResume(
            e -> {
              log.warn("Plum getAllUserCards ishlamadi, lokal DB fallback: {}", e.getMessage());
              return userCardRepository.findByUserIn(userIn).map(plumMapper::toCardResponse);
            });
  }

  // Lokal billing.plum_cards'ni Plum ro'yxatiga moslaydi (idempotent reconcile).
  private Flux<CardResponse> reconcileCards(String userIn, java.util.List<PlumCardInfo> plum) {
    return userCardRepository
        .findByUserIn(userIn)
        .collectList()
        .flatMapMany(
            local -> {
              java.util.Set<Long> plumIds = new java.util.HashSet<>();
              for (PlumCardInfo pc : plum) if (pc.cardId() != null) plumIds.add(pc.cardId());
              java.util.Set<Long> localIds = new java.util.HashSet<>();
              for (UserCardEntity e : local) if (e.getCardId() != null) localIds.add(e.getCardId());
              // 1) Lokalda bor, Plum'da yo'q -> o'chir (yetim tozalash)
              Flux<Void> del =
                  Flux.fromIterable(local)
                      .filter(e -> e.getCardId() == null || !plumIds.contains(e.getCardId()))
                      .flatMap(userCardRepository::delete);
              // 2) Plum'da bor, lokalda yo'q -> qo'sh
              Flux<UserCardEntity> ins =
                  Flux.fromIterable(plum)
                      .filter(pc -> pc.cardId() != null && !localIds.contains(pc.cardId()))
                      .flatMap(
                          pc ->
                              userCardRepository.save(
                                  UserCardEntity.builder()
                                      .userIn(userIn)
                                      .cardId(pc.cardId())
                                      .cardNumber(pc.number())
                                      .expireDate(toYySlashMm(pc.expireDate()))
                                      .type(CardType.detect(pc.number()))
                                      .status(pc.status())
                                      .build()));
              return del.thenMany(ins)
                  .thenMany(userCardRepository.findByUserIn(userIn).map(plumMapper::toCardResponse));
            });
  }

  // ===== Orphan tozalash: Plum'da bor, bizning DB'da yo'q kartani Plum'dan o'chirish =====

  /**
   * Plum'ning O'ZIDAGI kartalar ro'yxatini PINFL bo'yicha oladi (bizning DB emas).
   * Javob shakli aniq hujjatlashtirilmagani uchun JsonNode bilan moslashuvchan o'qiymiz:
   * result massiv yoki result.cards massivini qabul qilamiz.
   */
  public Flux<PlumCardInfo> getUserCardsFromPlum(String pinfl) {
    return plumWebClient()
        .flatMap(
            client ->
                client
                    .get()
                    .uri(uriBuilder -> uriBuilder.path("/UserCard/getAllUserCards")
                        .queryParam("userId", pinfl).build())
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, this::plumError)
                    .bodyToMono(String.class))
        // VAQTINCHALIK: Plum getUserCards xom javobini log qilamiz (endpoint/shakl tasdiqlash).
        .doOnNext(body -> log.info("PLUM getUserCards raw response: {}", body))
        .flatMapMany(this::parsePlumCards);
  }

  // Plum javobidan kartalarni ajratadi (result[] yoki result.cards[]).
  private Flux<PlumCardInfo> parsePlumCards(String body) {
    try {
      com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(body);
      com.fasterxml.jackson.databind.JsonNode result = root.path("result");
      com.fasterxml.jackson.databind.JsonNode arr =
          result.isArray() ? result : result.path("cards");
      // Kutilgan shakl (result[] yoki result.cards[]) topilmasa — bu "0 ta karta" emas,
      // javob formati noma'lum/o'zgargan degani. Xato tashlaymiz (reconcile chaqirilmaydi,
      // getUserCards fallback qilib lokal DB'ni qaytaradi) — aks holda haqiqiy kartalar
      // "yetim" deb o'chirilib ketadi.
      if (!arr.isArray()) {
        log.error("Plum getUserCards kutilmagan javob shakli (result/result.cards array emas): {}", body);
        return Flux.error(new BadRequestException("Plum kartalar javobi kutilmagan shaklda"));
      }
      java.util.List<PlumCardInfo> cards = new java.util.ArrayList<>();
      for (com.fasterxml.jackson.databind.JsonNode c : arr) {
        // `id` — Plum userCard bog'lam id'si (deleteUserCard SHUNI kutadi),
        // `cardId` — karta id'si (scoring shu bilan ishlaydi). Ikkalasi ham kerak.
        Long userCardId = c.hasNonNull("id") ? c.get("id").asLong() : null;
        Long cardId = c.hasNonNull("cardId") ? c.get("cardId").asLong() : userCardId;
        String number = c.hasNonNull("number") ? c.get("number").asText() : null;
        String expireDate = c.hasNonNull("expireDate") ? c.get("expireDate").asText() : null;
        Integer status = c.hasNonNull("status") ? c.get("status").asInt() : null;
        cards.add(new PlumCardInfo(userCardId, cardId, number, expireDate, status));
      }
      return Flux.fromIterable(cards);
    } catch (Exception e) {
      log.error("Plum getUserCards javobini parse qilishda xato: {}", e.getMessage());
      return Flux.error(new BadRequestException("Plum kartalar ro'yxatini o'qib bo'lmadi"));
    }
  }

  /**
   * Orphan kartani tozalaydi: Plum'dan PINFL bo'yicha ro'yxat olib, karta raqamiga
   * mos kartani Plum'ning ichki cardId'si bilan o'chiradi + agar lokal DB'da yozuvi
   * bo'lsa uni ham tozalaydi. Mos karta topilmasa xato.
   */
  public Mono<DeleteUserCardResponse> cleanOrphanCard(String pinfl, String cardNumber) {
    String pan = cardNumber == null ? "" : cardNumber.replaceAll("\\D", "");
    return getUserCardsFromPlum(pinfl)
        .filter(c -> c.userCardId() != null && panMatches(c.number(), pan))
        .next()
        .switchIfEmpty(
            Mono.error(new NotFoundException("Plum'da bu PINFL ostida bunday karta topilmadi")))
        .flatMap(match -> deleteOrphanFromPlum(match.userCardId(), pinfl, pan));
  }

  // Plum userCard `id` bilan to'g'ridan-to'g'ri Plum'dan o'chiradi (lokal DB shart emas);
  // lokal yozuv bo'lsa uni ham tozalaydi.
  private Mono<DeleteUserCardResponse> deleteOrphanFromPlum(
      Long plumUserCardId, String pinfl, String pan) {
    return deleteFromPlum(plumUserCardId)
        .flatMap(
            response -> {
              sendPlumLog(null, null, "CARD_DELETE_ORPHAN", "SUCCESS", null,
                  "pinfl=" + pinfl + " userCardId=" + plumUserCardId, toJson(response));
              // Lokal DB'da tasodifan yozuvi bo'lsa (masalan boshqa user_in) — tozalaymiz.
              return userCardRepository
                  .findByUserIn(pinfl)
                  .filter(e -> panMatches(e.getCardNumber(), pan))
                  .flatMap(userCardRepository::delete)
                  .then(Mono.just(response));
            })
        .doOnError(
            error ->
                sendPlumLog(null, null, "CARD_DELETE_ORPHAN", "ERROR", error.getMessage(),
                    "pinfl=" + pinfl + " userCardId=" + plumUserCardId, null));
  }

  // Karta raqamlari mosligini tekshiradi (faqat raqamlar bo'yicha, niqoblangan bo'lsa
  // oxirgi 4 raqam bilan ham).
  private static boolean panMatches(String a, String b) {
    if (a == null || b == null) return false;
    String da = a.replaceAll("\\D", "");
    String db = b.replaceAll("\\D", "");
    if (da.isEmpty() || db.isEmpty()) return false;
    if (da.equals(db)) return true;
    // Niqoblangan raqamlarda oxirgi 4 raqam bilan solishtiramiz.
    return da.length() >= 4 && db.length() >= 4
        && da.substring(da.length() - 4).equals(db.substring(db.length() - 4));
  }

  // Plum'dagi karta (ro'yxatdan): userCardId = ro'yxatdagi `id` (delete uchun),
  // cardId = karta id'si (scoring/reconcile uchun).
  public record PlumCardInfo(
      Long userCardId, Long cardId, String number, String expireDate, Integer status) {}

  public Mono<ScoringStatusResponse> createScoringCard(CreateScoringCardRequest request, String requesterIn) {
    log.info("Creating scoring card for cardId: {}", request.cardId());

    return userCardRepository
        .findById(request.cardId())
        .switchIfEmpty(Mono.error(new NotFoundException("Card not found")))
        .flatMap(
            card -> {
              // Saqlangan tur UNKNOWN bo'lsa, karta raqamidan qayta aniqlaymiz —
              // eski yozuvlarda 6262 kabi BIN'lar UNKNOWN saqlangan bo'lishi mumkin.
              CardType type =
                  card.getType() == CardType.UNKNOWN
                      ? CardType.detect(card.getCardNumber())
                      : card.getType();
              Mono<ScoringStatusResponse> resultMono;
              if (type == CardType.UZCARD) {
                resultMono = uzcardScoring(card, request, requesterIn).cast(ScoringStatusResponse.class);
              } else {
                resultMono =
                    humoScoring(
                            new HumoScoringRequestPlum(
                                card.getCardId(), request.beginDate(), request.endDate()),
                            null,
                            card.getId())
                        .flatMap(
                            scoring -> {
                              PlumScoringEntity entity = new PlumScoringEntity();
                              entity.setUserIn(card.getUserIn());
                              entity.setRequesterIn(requesterIn);
                              entity.setCardId(card.getId());
                              entity.setCreatedAt(Instant.now());
                              entity.setType(ScoringType.HUMO);
                              entity.setStatus(ScoringStatus.COMPLETED);
                              Map<String, Object> map =
                                  objectMapper.convertValue(scoring, new TypeReference<>() {});
                              entity.setUzcard(map);
                              return scoringRepository
                                  .save(entity)
                                  .map(plumMapper::toScoringStatusResponse);
                            });
              }
              // Scoring muvaffaqiyatli boshlangach usage'ni main'ga yozamiz (per-tur PAYMENT,
              // karta skoringi). Best-effort — oqimni buzmaydi.
              // Har scoringdan keyin: 1) usage (kvota, paketga) + 2) umumlashgan jurnal
              // (user_scoring — bitta odamning barcha scoringlarini jamlaydi). Best-effort.
              return resultMono.flatMap(
                  resp -> {
                    String ref = resp.id() != null ? resp.id().toString() : null;
                    return webUserServiceClient
                        .recordScoringUsage(
                            card.getUserIn(), request.userPackageId(), "PAYMENT", ref)
                        .then(
                            webUserServiceClient.recordUserScoring(
                                ref, "PAYMENT", requesterIn, card.getUserIn()))
                        .thenReturn(resp);
                  });
            });
  }

  public Mono<HumoScoringResponse> humoScoring(
      HumoScoringRequestPlum request, UUID userId, UUID cardId) {
    log.info("Humo scoring for cardId: {}", request.cardId());
    String requestBody = toJson(request);
    return plumWebClient()
        .flatMap(
            client ->
                client
                    .post()
                    .uri("/Scoring/HumoScoring")
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, this::plumError)
                    .bodyToMono(HumoScoringResponse.class))
        .doOnSuccess(
            response -> {
              log.info("Humo scoring retrieved");
              sendPlumLog(userId, cardId, "HUMO", "SUCCESS", null, requestBody, toJson(response));
            })
        .doOnError(
            error -> {
              log.error("Error getting Humo scoring", error);
              sendPlumLog(
                  userId, cardId, "HUMO", "ERROR", error.getMessage(), requestBody, null);
            });
  }

  // Backward-compat: userId/cardId'siz chaqiruvlar uchun (log'siz).
  public Mono<HumoScoringResponse> humoScoring(HumoScoringRequestPlum request) {
    return humoScoring(request, null, null);
  }

  public Mono<ScoringStatusResponse> getScoringStatus(UUID id) {
    return scoringRepository
        .findById(id)
        .switchIfEmpty(Mono.error(new NotFoundException("Scoring not found")))
        .map(plumMapper::toScoringStatusResponse);
  }

  // Tarix PINFL/STIR (user_in) bo'yicha — pagination qo'lda (LIMIT/OFFSET).
  public Flux<ScoringResponse> getUserScoringHistory(String userIn, UUID cardId, Pageable pageable) {
    log.debug("Find all user scoring history {}", userIn);
    int limit = pageable.getPageSize();
    long offset = pageable.getOffset();
    if (cardId != null) {
      return scoringRepository
          .findByUserInAndCardId(userIn, cardId, limit, offset)
          .map(plumMapper::toScoringResponse);
    }
    return scoringRepository.findByUserIn(userIn, limit, offset).map(plumMapper::toScoringResponse);
  }

  // Faqat SO'ROVCHI (requester) o'zi qilgan skoringlar — boshqalarnikini ko'rmaydi.
  public Flux<ScoringResponse> getMyScoringHistory(String requesterIn, UUID cardId, Pageable pageable) {
    log.debug("Find requester scoring history {}", requesterIn);
    int limit = pageable.getPageSize();
    long offset = pageable.getOffset();
    if (cardId != null) {
      return scoringRepository
          .findByRequesterInAndCardId(requesterIn, cardId, limit, offset)
          .map(plumMapper::toScoringResponse);
    }
    return scoringRepository
        .findByRequesterIn(requesterIn, limit, offset)
        .map(plumMapper::toScoringResponse);
  }

  // ===================== Payment (kartadan pul yechib balansga qo'shish) =====================

  // Biriktirilgan kartadan to'lov. Karta trusted bo'lsa darhol bajariladi va balansga qo'shiladi;
  // bo'lmasa Plum OTP yuboradi (otpRequired=true) — keyin confirmPayment(session, otp) chaqiriladi.
  public Mono<PlumPaymentResponse> payment(PlumPaymentRequest request) {
    if (request.cardId() == null) {
      return Mono.error(new BadRequestException("cardId is required"));
    }
    if (request.amount() == null || request.amount() <= 0) {
      return Mono.error(new BadRequestException("amount must be positive"));
    }
    return userCardRepository
        .findById(request.cardId())
        .switchIfEmpty(Mono.error(new NotFoundException("Card not found")))
        .flatMap(card -> chargeAndTopUp(card, request));
  }

  // Plum'ga to'lov so'rovini yuboradi; muvaffaqiyatda balansga qo'shadi va natijani qaytaradi.
  private Mono<PlumPaymentResponse> chargeAndTopUp(UserCardEntity card, PlumPaymentRequest request) {
    // Pul-xavfsizlik: balans uchun user UUID'ni PINFL'dan ZARYADDAN OLDIN resolish —
    // topilmasa Plum'ga yuborilmaydi (charged-but-not-credited holatidan qochish).
    return webUserServiceClient
        .resolveUserIdByPinfl(card.getUserIn())
        .switchIfEmpty(Mono.error(new BadRequestException("Karta egasi topilmadi")))
        .flatMap(userId -> doChargeAndTopUp(card, request, userId));
  }

  private Mono<PlumPaymentResponse> doChargeAndTopUp(
      UserCardEntity card, PlumPaymentRequest request, UUID userId) {
    String extraId = UUID.randomUUID().toString();
    PaymentRequestPlum plumRequest =
        new PaymentRequestPlum(
            card.getUserIn(), // Plum saqlangan identifikator = PINFL
            card.getCardId(),
            request.amount(),
            extraId,
            Boolean.FALSE,
            request.transactionData());
    String requestBody = toJson(plumRequest);
    return plumWebClient()
        .flatMap(
            client ->
                client
                    .post()
                    .uri("/Payment/payment")
                    .bodyValue(plumRequest)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, this::plumError)
                    .bodyToMono(PaymentResponse.class))
        .switchIfEmpty(Mono.error(new BadRequestException("Plum service'dan bo'sh javob keldi")))
        .flatMap(
            response -> {
              if (response.error() != null) {
                return Mono.error(new BadRequestException(response.error().errorMessage()));
              }
              var r = response.result();
              if (r == null) {
                return Mono.error(new BadRequestException("Plum service'dan bo'sh natija keldi"));
              }
              // Karta trusted bo'lmasa Plum OTP yuboradi (utrno=null, session to'la) — balansga
              // qo'shilmaydi; mijoz confirmPayment(session, otp) chaqirishi kerak.
              if (r.utrno() == null) {
                if (r.session() == null) {
                  return Mono.error(new BadRequestException("To'lov yakunlanmadi"));
                }
                sendPlumLog(
                    null, card.getId(), "PAYMENT", "OTP_SENT", null, requestBody, toJson(response));
                return Mono.just(
                    PlumPaymentResponse.otpRequired(
                        r.session(), r.otpSentPhone(), r.transactionId(), request.amount()));
              }
              // Trusted karta — darhol bajarilgan, balansga qo'shamiz.
              return topUpBalance(userId, request.amount())
                  .map(
                      newBalance -> {
                        sendPlumLog(
                            null,
                            card.getId(),
                            "PAYMENT",
                            "SUCCESS",
                            null,
                            requestBody,
                            toJson(response));
                        return PlumPaymentResponse.completed(
                            r.transactionId(),
                            r.utrno(),
                            r.cardNumber(),
                            request.amount(),
                            newBalance);
                      });
            })
        .doOnError(
            error -> {
              log.error("Error during plum payment", error);
              sendPlumLog(
                  null, card.getId(), "PAYMENT", "ERROR", error.getMessage(), requestBody, null);
            });
  }

  // To'lov muvaffaqiyatli bo'lgach C2C foydalanuvchi balansiga qo'shadi (Payme/Click pattern).
  private Mono<Double> topUpBalance(UUID userId, Double amount) {
    return balanceHelper
        .getBalanceEntity(userId, BalanceType.SUMMA, BillingType.C2C)
        .flatMap(
            balance -> {
              TransactionEntity tx = new TransactionEntity();
              tx.setBillingType(BillingType.C2C);
              tx.setUserId(userId);
              tx.setType(TransactionType.DEPOSIT_PLUM);
              tx.setAmount(amount);
              tx.setBalanceId(balance.getId());
              return transactionRepository
                  .save(tx)
                  .flatMap(
                      saved -> {
                        balance.setBalance(balance.getBalance() + amount);
                        return paymentRecorder
                            .record(userId, amount, PaymentMethod.PLUM)
                            .then(balanceRepository.save(balance));
                      })
                  .map(BalanceEntity::getBalance);
            });
  }

  // To'lovni OTP bilan tasdiqlaydi (trusted bo'lmagan karta). status=1 (muvaffaqiyat) bo'lsa C2C
  // balansga qo'shadi. Balansni to'g'ri to'ldirish uchun cardId + amount Plum javobidan olinadi.
  public Mono<PlumPaymentResponse> confirmPayment(PaymentConfirmRequest request) {
    if (request.session() == null || request.otp() == null) {
      return Mono.error(new BadRequestException("session va otp talab qilinadi"));
    }
    String requestBody = toJson(request);
    return plumWebClient()
        .flatMap(
            client ->
                client
                    .post()
                    .uri("/Payment/confirmPayment")
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, this::plumError)
                    .bodyToMono(ConfirmPaymentResponse.class))
        .switchIfEmpty(Mono.error(new BadRequestException("Plum service'dan bo'sh javob keldi")))
        .flatMap(
            response -> {
              if (response.error() != null) {
                return Mono.error(new BadRequestException(response.error().errorMessage()));
              }
              var r = response.result();
              // status=1 — muvaffaqiyat (4.1 STATUSY TRANZAKSIY). Aks holda balansga qo'shilmaydi.
              if (r == null || r.status() == null || r.status() != 1) {
                String msg =
                    (r != null && r.statusComment() != null)
                        ? r.statusComment()
                        : "To'lov tasdiqlanmadi";
                return Mono.error(new BadRequestException(msg));
              }
              return userCardRepository
                  .findByCardId(r.cardId())
                  .switchIfEmpty(Mono.error(new NotFoundException("Card not found")))
                  .flatMap(
                      card ->
                          webUserServiceClient
                              .resolveUserIdByPinfl(card.getUserIn())
                              .switchIfEmpty(
                                  Mono.error(
                                      new BadRequestException("Karta egasi topilmadi (balans to'lmadi)")))
                              .flatMap(
                                  userId ->
                                      topUpBalance(userId, r.amount())
                                          .map(
                                              newBalance -> {
                                                sendPlumLog(
                                                    null,
                                                    card.getId(),
                                                    "PAYMENT_CONFIRM",
                                                    "SUCCESS",
                                                    null,
                                                    requestBody,
                                                    toJson(response));
                                                return PlumPaymentResponse.completed(
                                                    r.transactionId(),
                                                    r.utrno(),
                                                    r.cardNumber(),
                                                    r.amount(),
                                                    newBalance);
                                              })));
            })
        .doOnError(
            error -> {
              log.error("Error during plum payment confirm", error);
              sendPlumLog(
                  null, null, "PAYMENT_CONFIRM", "ERROR", error.getMessage(), requestBody, null);
            });
  }

  // Plum xato kodlarini foydalanuvchiga tushunarli (o'zbekcha) xabarga o'giradi;
  // noma'lum kodlarda Plum'ning asl xabarini qaytaradi.
  private static String friendlyPlumMessage(uz.hesap.service.integration.model.uzcard.ErrorResponse err) {
    if (err == null) {
      return "Karta xizmatida xatolik";
    }
    Integer code = err.errorCode();
    if (code != null && code == -108) {
      return "Bu karta allaqachon qo'shilgan";
    }
    String msg = err.errorMessage();
    return (msg == null || msg.isBlank()) ? "Karta xizmatida xatolik" : msg;
  }

  private Mono<? extends Throwable> plumError(ClientResponse clientResponse) {
    log.error("Error plum : {}", clientResponse.statusCode());
    if (clientResponse.statusCode().value() == 403) {
      return Mono.error(new ForbiddenException("Access denied to PLUM service"));
    }

    return clientResponse
        .bodyToMono(String.class)
        .flatMap(
            errorBody -> {
              try {
                ScoringGetPointResponse error =
                    objectMapper.readValue(errorBody, ScoringGetPointResponse.class);
                log.error("Error plum {} ", error.error());
                return Mono.error(new BadRequestException(friendlyPlumMessage(error.error())));
              } catch (JsonProcessingException e) {
                return Mono.error(new RuntimeException("PLUM Error: " + errorBody));
              }
            });
  }

  private Mono<? extends ScoringStatusResponse> uzcardScoring(
      UserCardEntity card, CreateScoringCardRequest request, String requesterIn) {
    CreateScoringCardRequestPlum plumRequest =
        new CreateScoringCardRequestPlum(
            card.getCardId(), 114L, request.beginDate(), request.endDate());
    String requestBody = toJson(plumRequest);
    return plumWebClient()
        .flatMap(
            client ->
                client
                    .post()
                    .uri("/Scoring/createScoringCard")
                    .bodyValue(plumRequest)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, this::plumError)
                    .bodyToMono(CreateScoringCardResponse.class))
        .doOnSuccess(
            response -> {
              log.info(
                  "Scoring card created, scoringId: {}",
                  response.result() != null ? response.result().scoringId() : "null");
              sendPlumLog(
                  null, card.getId(), "UZCARD", "SUCCESS", null, requestBody, toJson(response));
            })
        .doOnError(
            error -> {
              log.error("Error creating scoring card", error);
              sendPlumLog(
                  null, card.getId(), "UZCARD", "ERROR", error.getMessage(), requestBody, null);
            })
        .flatMap(
            res -> {
              PlumScoringEntity entity = new PlumScoringEntity();
              entity.setUserIn(card.getUserIn());
              entity.setRequesterIn(requesterIn);
              entity.setCardId(card.getId());
              entity.setCreatedAt(Instant.now());
              entity.setType(ScoringType.UZCARD);
              entity.setPlumScoringId(res.result().scoringId());
              entity.setStatus(ScoringStatus.IN_PROGRESS);
              return scoringRepository.save(entity).map(plumMapper::toScoringStatusResponse);
            });
  }

  // Plum so'rovi logini log-servisga (RabbitMQ) yuboradi. Log yozish asosiy
  // skoring oqimini to'xtatmasligi kerak — xato bo'lsa yutiladi.
  private void sendPlumLog(
      final UUID userId,
      final UUID cardId,
      final String type,
      final String status,
      final String errorMessage,
      final String request,
      final String response) {
    jmsPublisher
        .publish(
            new PlumLogReply(
                userId, cardId, type, status, errorMessage, request, response, Instant.now()))
        .subscribe();
  }

  // Obyektni JSON string'ga aylantiradi; xato bo'lsa null (log to'xtab qolmasin).
  private String toJson(final Object value) {
    if (value == null) return null;
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      log.warn("Plum log JSON serialization failed: {}", e.getMessage());
      return null;
    }
  }
}
