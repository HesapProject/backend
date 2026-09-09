package uz.hesap.service.main.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.enums.Permission;
import uz.hesap.service.common.util.enums.Role;
import uz.hesap.service.common.util.enums.UserType;
import uz.hesap.service.common.util.message.OneIdLogReply;
import uz.hesap.service.jms.JmsPublisher;
import uz.hesap.service.main.context.WebClientConfig;
import uz.hesap.service.main.domain.SessionEntity;
import uz.hesap.service.main.domain.StaffEntity;
import uz.hesap.service.main.domain.UserEntity;
import uz.hesap.service.main.domain.enums.StaffStatus;
import uz.hesap.service.main.domain.enums.StaffType;
import uz.hesap.service.main.feign.IntegrationServiceClient;
import uz.hesap.service.main.model.mapper.SessionMapper;
import uz.hesap.service.main.model.request.ClientOneIdVerifyRequest;
import uz.hesap.service.main.model.response.JwtTokenResponse;
import uz.hesap.service.main.model.response.OneIdUserResponse;
import uz.hesap.service.main.repository.SessionRepository;
import uz.hesap.service.main.repository.StaffRepository;
import uz.hesap.service.main.repository.UserRepository;

@Log4j2
@Service
public class OneIdService {

  private final UserRepository userRepository;
  private final StaffRepository staffRepository;
  private final SessionRepository sessionRepository;
  private final JwtService jwtService;
  // sso.egov.uz bilan barcha HTTP integration servisida. main-service faqat
  // User/Device/Session/JWT orchestratori — Feign'dan tashqari HTTP yo'q.
  private final IntegrationServiceClient integrationClient;
  private final JmsPublisher jmsPublisher;
  private final ObjectMapper objectMapper;
  private final UserRegisteredNotifier userRegisteredNotifier;

  public OneIdService(
      UserRepository userRepository,
      StaffRepository staffRepository,
      SessionRepository sessionRepository,
      JwtService jwtService,
      IntegrationServiceClient integrationClient,
      JmsPublisher jmsPublisher,
      ObjectMapper objectMapper,
      UserRegisteredNotifier userRegisteredNotifier) {
    this.userRepository = userRepository;
    this.staffRepository = staffRepository;
    this.sessionRepository = sessionRepository;
    this.jwtService = jwtService;
    this.integrationClient = integrationClient;
    this.jmsPublisher = jmsPublisher;
    this.objectMapper = objectMapper;
    this.userRegisteredNotifier = userRegisteredNotifier;
  }

  public Mono<String> getUrl(String redirectUrl) {
    return integrationClient.getOneIdLoginUrl(redirectUrl);
  }

  // OneID bilan verify → user saqlash + oneIdUser saqlash + device + session + JWT
  public Mono<JwtTokenResponse> verifyClient(ClientOneIdVerifyRequest request) {
    // [DIAGNOSTIC 1] verify chaqirig'i kelishi
    log.info(
        "[ONEID-VERIFY] start: code={}, redirectUrl={}, uuid={}",
        request.code(),
        request.redirectUrl(),
        request.uuid());

    return integrationClient
        .exchangeOneIdCode(request.code(), request.redirectUrl())
        .flatMap(
            oneIdResponse ->
                saveOrUpdateUser(oneIdResponse, request.redirectUrl())
                    .flatMap(
                        user -> saveOneIdUserData(user.getId(), oneIdResponse).thenReturn(user))
                    // OneID so'rovi va javobini log servisiga yozamiz (req/resp).
                    .flatMap(
                        user -> logOneIdRequest(request, oneIdResponse, user).thenReturn(user)))
        .flatMap(user -> createSessionAndToken(user, request));
  }

  // OneID verify so'rovi va javobini log servisiga (RabbitMQ orqali) yuboradi.
  // Log yozish asosiy oqimni to'xtatmasligi kerak — xato bo'lsa yutiladi.
  private Mono<Void> logOneIdRequest(
      ClientOneIdVerifyRequest request, OneIdUserResponse response, UserEntity user) {
    var requestMap = new HashMap<String, Object>();
    requestMap.put("code", request.code());
    requestMap.put("redirectUrl", request.redirectUrl());
    OneIdLogReply reply =
        new OneIdLogReply(
            user.getId(),
            user.getFirstName(),
            user.getLastName(),
            null,
            toJson(requestMap),
            toJson(response),
            "SUCCESS",
            null,
            Instant.now());
    return jmsPublisher
        .publish(reply)
        .then()
        .onErrorResume(
            e -> {
              log.warn("OneID log publish failed: {}", e.getMessage());
              return Mono.empty();
            });
  }

  // Obyektni JSON string'ga aylantiradi; xato bo'lsa null.
  private String toJson(final Object value) {
    if (value == null) return null;
    try {
      return objectMapper.writeValueAsString(value);
    } catch (final Exception e) {
      log.warn("OneID log JSON serialization failed: {}", e.getMessage());
      return null;
    }
  }

  // OneID javobini turiga qarab branch qiladi:
  // - isLegal=true → COMPANY user (identifier: pkcsLegalTin, 9-xonali)
  // - aks holda → CLIENT user (identifier: pin/PINFL, 14-xonali)
  // Ikkala holatda `pinfl` kolonkasiga identifier yoziladi (schema
  // migration'siz ko'cha olish uchun).
  private Mono<UserEntity> saveOrUpdateUser(OneIdUserResponse oneIdResponse, String redirectUrl) {
    // Yuridik (COMPANY) ro'yxat FAQAT business ilovada. clientWeb (jismoniy shaxslar
    // ilovasi) doim CLIENT — legalInfo/is_legal bo'lsa ham (clientWeb faqat jismoniy uchun).
    boolean isBusinessApp = isBusinessApp(redirectUrl);
    boolean isLegal =
        isBusinessApp
            && (Boolean.TRUE.equals(oneIdResponse.isLegal()) || hasLegalInfo(oneIdResponse));

    // [DIAGNOSTIC 2] OneID javobining muhim field'lari
    log.info(
        "[ONEID-RESPONSE] isBusinessApp={}, isLegal={}, pin={}, pkcsLegalTin={}, authMethod={},"
            + " legalInfoSize={}, legalInfo={}",
        isBusinessApp,
        oneIdResponse.isLegal(),
        oneIdResponse.pin(),
        oneIdResponse.pkcsLegalTin(),
        oneIdResponse.authMethod(),
        oneIdResponse.legalInfo() != null ? oneIdResponse.legalInfo().size() : 0,
        oneIdResponse.legalInfo());

    // [DIAGNOSTIC 3] Branch tanlovi — qaysi yo'l ketdi
    log.info("[ONEID-BRANCH] isLegal={} → {}", isLegal, isLegal ? "COMPANY" : "CLIENT");

    if (isLegal) {
      return registerLegalAndPerson(oneIdResponse);
    }
    return saveOrUpdateClientUser(oneIdResponse);
  }

  // Yuridik shaxs orqali login: ikkala shaxsni ham tizimga kiritadi —
  // yuridik shaxs (COMPANY) + jismoniy shaxs (CLIENT) — va jismoniy shaxsni
  // yuridik shaxsga OWNER qilib bog'laydi. Sessiya egasi sifatida jismoniy
  // shaxs (login qilgan odam) qaytariladi.
  private Mono<UserEntity> registerLegalAndPerson(OneIdUserResponse oneIdResponse) {
    return saveOrUpdateLegalUser(oneIdResponse)
        .flatMap(
            company -> {
              // PKCS javobida jismoniy shaxs PINFL'i (pin) bo'lmasa — bo'sh
              // pinfl bilan soxta CLIENT yaratmaymiz, faqat yuridik shaxs qaytadi.
              if (oneIdResponse.pin() == null || oneIdResponse.pin().isBlank()) {
                log.warn("[ONEID-LEGAL] pin yo'q — jismoniy shaxs yaratilmadi, sessiya COMPANY'ga");
                return Mono.just(company);
              }
              return saveOrUpdateClientUser(oneIdResponse)
                  .flatMap(
                      person ->
                          linkPersonAsCompanyOwner(person.getId(), company.getId())
                              .thenReturn(person));
            });
  }

  // Jismoniy shaxsni yuridik shaxsga (COMPANY user) OWNER sifatida `staff` orqali bog'laydi.
  // company_id COMPANY UserEntity id'siga ishora qiladi (FK yo'q). Allaqachon bog'langan
  // bo'lsa — qayta yaratmaydi. OWNER barcha ruxsatlarni oladi va darhol ACCEPTED bo'ladi.
  private Mono<Void> linkPersonAsCompanyOwner(UUID personUserId, UUID companyUserId) {
    return staffRepository
        .findFirstByUserIdAndCompanyIdAndDeletedFalse(personUserId, companyUserId)
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  StaffEntity staff = new StaffEntity();
                  staff.setUserId(personUserId);
                  staff.setCompanyId(companyUserId);
                  staff.setType(StaffType.OWNER);
                  staff.setStatus(StaffStatus.ACCEPTED);
                  staff.setPermissions(Permission.values());
                  staff.setCreatedBy(personUserId);
                  return staffRepository.save(staff);
                }))
        .then();
  }

  // CLIENT (jismoniy shaxs) — pin/PINFL bo'yicha qidiradi, yo'q bo'lsa yaratadi.
  // `findFirstBy…OrderBy…` LIMIT 1 qo'shadi — test DB'dagi duplicate
  // qatorlar uchun "returned non unique result" xatosini oldini oladi.
  private Mono<UserEntity> saveOrUpdateClientUser(OneIdUserResponse oneIdResponse) {
    String in = oneIdResponse.pin();
    return userRepository
        .findFirstByInAndTypeAndDeletedFalseOrderByCreatedDateAsc(in, UserType.CLIENT)
        .switchIfEmpty(Mono.defer(() -> createNewClientUser(oneIdResponse)))
        .flatMap(
            user -> {
              // id == null → yangi mijoz (createNewClientUser saqlanmagan entity qaytaradi).
              boolean isNew = user.getId() == null;
              user.setPassport(oneIdResponse.document());
              user.setFirstName(oneIdResponse.nameLatin());
              user.setLastName(oneIdResponse.surnameLatin());
              user.setMidName(oneIdResponse.patronymicLatin());
              if (oneIdResponse.birthDate() != null) {
                user.setBirthday(oneIdResponse.birthDate());
              }
              // Profilda ko'rsatish uchun: manzil, tug'ilgan joy, millat, fuqarolik.
              // Bo'sh kelsa eski qiymat o'chmaydi.
              if (oneIdResponse.address() != null && !oneIdResponse.address().isBlank()) {
                user.setAddress(oneIdResponse.address());
              }
              if (oneIdResponse.region() != null && !oneIdResponse.region().isBlank()) {
                user.setRegion(oneIdResponse.region());
              }
              if (oneIdResponse.district() != null && !oneIdResponse.district().isBlank()) {
                user.setDistrict(oneIdResponse.district());
              }
              if (oneIdResponse.birthPlace() != null && !oneIdResponse.birthPlace().isBlank()) {
                user.setBirthPlace(oneIdResponse.birthPlace());
              }
              if (oneIdResponse.nationality() != null && !oneIdResponse.nationality().isBlank()) {
                user.setNationality(oneIdResponse.nationality());
              }
              if (oneIdResponse.citizenship() != null && !oneIdResponse.citizenship().isBlank()) {
                user.setCitizenship(oneIdResponse.citizenship());
              }
              // OneID login isVerified bermaydi — jismoniy shaxs keyin MyID orqali
              // tasdiqlaydi. Yangi user default FALSE, mavjud qiymat saqlanadi.
              // Yangi mijoz bo'lsa — amoCRM'ga (RabbitMQ orqali) xabar (fire-and-forget).
              return userRepository
                  .save(user)
                  .flatMap(
                      saved ->
                          isNew
                              ? userRegisteredNotifier.notifyRegistered(saved).thenReturn(saved)
                              : Mono.just(saved));
            });
  }

  // COMPANY (yuridik shaxs) — TIN (pkcsLegalTin, 9-xonali) bo'yicha qidiradi.
  // legal_name va tin ustunlariga yoziladi (firstName/pinfl emas).
  private Mono<UserEntity> saveOrUpdateLegalUser(OneIdUserResponse oneIdResponse) {
    String tin = resolveLegalTin(oneIdResponse);
    return userRepository
        .findFirstByTinAndTypeAndDeletedFalseOrderByCreatedDateAsc(tin, UserType.COMPANY)
        .switchIfEmpty(Mono.defer(() -> createNewLegalUser(oneIdResponse)))
        .flatMap(
            user -> {
              user.setTin(tin);
              // Identifikator (in/pinfl ustuni) — yuridik shaxs uchun TIN.
              // Jismoniy shaxsda PINFL, yuridikda TIN — universal identifikator.
              user.setIn(tin);
              String legalName = extractLegalName(oneIdResponse);
              if (legalName != null && !legalName.isBlank()) {
                user.setLegalName(legalName);
              }
              // OneID login isVerified bermaydi — yuridik shaxs keyin e-imzo orqali
              // tasdiqlaydi. Yangi user default FALSE, mavjud qiymat saqlanadi.
              return userRepository.save(user);
            });
  }

  private static String extractLegalName(OneIdUserResponse response) {
    if (response.legalInfo() == null || response.legalInfo().isEmpty()) return null;
    var info = pickLegalInfo(response);
    return info.leName() != null ? info.leName() : info.acronUz();
  }

  // legalInfo bo'sh bo'lmasligi — yuridik shaxs belgisi.
  private static boolean hasLegalInfo(OneIdUserResponse response) {
    return response.legalInfo() != null && !response.legalInfo().isEmpty();
  }

  // Login business ilovasidanmi (business.hesap.uz / business-dev.hesap.uz)?
  // clientWeb (client.hesap.uz) emas — yuridik ro'yxat faqat business uchun.
  private static boolean isBusinessApp(String redirectUrl) {
    return redirectUrl != null && redirectUrl.contains("business");
  }

  // Asosiy (is_basic) yuridik shaxs yozuvi; bo'lmasa birinchisi.
  private static OneIdUserResponse.LegalInfo pickLegalInfo(OneIdUserResponse response) {
    return response.legalInfo().stream()
        .filter(OneIdUserResponse.LegalInfo::isBasic)
        .findFirst()
        .orElse(response.legalInfo().get(0));
  }

  // Yuridik TIN: avval pkcsLegalTin (9-xonali), bo'lmasa legalInfo'dan (tin yoki le_tin).
  private static String resolveLegalTin(OneIdUserResponse response) {
    if (response.pkcsLegalTin() != null && !response.pkcsLegalTin().isBlank()) {
      return response.pkcsLegalTin();
    }
    if (!hasLegalInfo(response)) return null;
    var info = pickLegalInfo(response);
    return info.tin() != null ? info.tin() : info.leTin();
  }

  // yangi CLIENT user
  private Mono<UserEntity> createNewClientUser(OneIdUserResponse oneIdResponse) {
    UserEntity user = new UserEntity();
    user.setIn(oneIdResponse.pin());
    user.setType(UserType.CLIENT);
    user.setRole(Role.USER);
    return Mono.just(user);
  }

  // yangi COMPANY user — TIN va legalName alohida ustunlarda saqlanadi
  private Mono<UserEntity> createNewLegalUser(OneIdUserResponse oneIdResponse) {
    UserEntity user = new UserEntity();
    String tin = resolveLegalTin(oneIdResponse);
    user.setTin(tin);
    // Identifikator (in/pinfl ustuni) — yuridik shaxs uchun TIN.
    user.setIn(tin);
    String legalName = extractLegalName(oneIdResponse);
    if (legalName != null && !legalName.isBlank()) {
      user.setLegalName(legalName);
    }
    user.setType(UserType.COMPANY);
    user.setRole(Role.USER);
    return Mono.just(user);
  }

  // OneID dan kelgan to'liq ma'lumotni alohida tablega saqlash (userId = PK).
  // OneID profili integration servisida saqlanadi (integration.one_id_user).
  // main-service Feign orqali shu xizmatga delegate qiladi.
  private Mono<Void> saveOneIdUserData(UUID userId, OneIdUserResponse response) {
    return integrationClient.saveOneIdProfile(userId, response);
  }

  // device saqlash + session yaratish + 30 kunlik JWT qaytarish
  private Mono<JwtTokenResponse> createSessionAndToken(
      UserEntity user, ClientOneIdVerifyRequest request) {
    SessionEntity session = SessionMapper.INSTANCE.toSession(request);
    session.setUserId(user.getId());
    return sessionRepository
        .deleteByUuid(session.getUuid())
        .then(sessionRepository.save(session))
        .map(
            saved ->
                new JwtTokenResponse(
                    jwtService.generateLongLivedToken(
                        saved.getUserId(), saved.getId(), "one-id")));
  }

  // sso.egov.uz HTTP integratsiyasi integration servisga ko'chirildi:
  // POST /api/integration/v1/local/oneid/exchange  →  IntegrationServiceClient.exchangeOneIdCode
}
