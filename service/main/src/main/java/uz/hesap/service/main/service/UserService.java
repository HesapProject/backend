package uz.hesap.service.main.service;

import static uz.hesap.service.common.exception.handler.ErrorCode.*;

import com.github.benmanes.caffeine.cache.Cache;
import java.security.SecureRandom;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.exception.AlreadyExistsException;
import uz.hesap.service.common.exception.ForbiddenException;
import uz.hesap.service.common.exception.InvalidArgumentException;
import uz.hesap.service.common.exception.NotFoundException;
import uz.hesap.service.common.util.CompanyResponse;
import uz.hesap.service.common.util.UserBasicResponse;
import uz.hesap.service.common.util.UserPassportBasicResponse;
import uz.hesap.service.common.util.UserPrincipal;
import uz.hesap.service.common.util.UserResponse;
import uz.hesap.service.common.util.enums.CompanyType;
import uz.hesap.service.common.util.enums.UserType;
import uz.hesap.service.main.domain.*;
import uz.hesap.service.main.domain.enums.StaffStatus;
import uz.hesap.service.main.feign.IntegrationServiceClient;
import uz.hesap.service.main.feign.NotificationServiceClient;
import uz.hesap.service.main.model.UserCacheModel;
import uz.hesap.service.main.model.UserWithRoleDto;
import uz.hesap.service.main.model.mapper.SessionMapper;
import uz.hesap.service.main.model.mapper.UserMapper;
import uz.hesap.service.main.model.request.ClientProfileUpdateRequest;
import uz.hesap.service.main.model.request.LoginRequest;
import uz.hesap.service.main.model.request.PhoneConfirmRequest;
import uz.hesap.service.main.model.request.PhoneSendCodeRequest;
import uz.hesap.service.main.model.request.UserRequest;
import uz.hesap.service.main.model.request.UserUpdateRequest;
import uz.hesap.service.main.model.response.AdminUserResponse;
import uz.hesap.service.main.model.response.AgeBucketCount;
import uz.hesap.service.main.model.response.JwtTokenResponse;
import uz.hesap.service.main.model.response.OneIdPassportResponse;
import uz.hesap.service.main.model.response.PhoneConfirmResponse;
import uz.hesap.service.main.repository.*;

@Log4j2
@Service
@RequiredArgsConstructor
public class UserService {
  private final UserRepository userRepository;
  private final UserRegisteredNotifier userRegisteredNotifier;
  private final SessionRepository sessionRepository;
  private final JwtService jwtService;
  private final CustomRepository customRepository;
  private final Cache<String, UserCacheModel> cachedUserOnSignUp;
  private final StaffRepository staffRepository;
  private final IntegrationServiceClient integrationServiceClient;
  private final SystemSettingService systemSettingService;
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  private static final String SIGN_UP_SMS_TEMPLATE =
      "Hesap.uz ilovasiga ro'yxatdan o'tish uchun tasdiqlash kod: %s";
  private static final String PHONE_VERIFY_SMS_TEMPLATE =
      "Hesap.uz ilovasiga ro'yxatdan o'tish uchun tasdiqlash kod: %s";
  private final NotificationServiceClient notificationServiceClient;
  private final Cache<String, String> cachedPhoneVerifyCode;

  @Value("${application.development:false}")
  private boolean isDevelopment;

  @Value("${application.test-phones:}")
  private String testPhones;

  // Client (jismoniy shaxs) qidirish — avval ClientController/ClientService'da edi.
  public Flux<UserResponse> searchClients(String search) {
    return userRepository.searchClients(search).map(UserMapper.INSTANCE::toUserResponse);
  }

  // Kompaniya xodimlari (staff a'zolari) — Xodimlar sahifasi. companyId frontenddan
  // (aktiv kompaniya) keladi; staff+user join, user detali + role bilan.
  public Mono<org.springframework.data.domain.Page<UserWithRoleDto>> getCompanyUsers(
      final UUID companyId, final String search, final org.springframework.data.domain.Pageable pageable) {
    Mono<List<UserWithRoleDto>> listMono =
        customRepository.getCompanyUsers(companyId, search, pageable).collectList();
    Mono<Long> countMono = customRepository.getCompanyUsersCount(companyId, search);
    return Mono.zip(listMono, countMono)
        .map(t -> new org.springframework.data.domain.PageImpl<>(t.getT1(), pageable, t.getT2()));
  }

  public Mono<UserResponse> createUser(final UserRequest userRequest) {
    log.debug("Create user: {}", userRequest);

    return userRepository
        .existsByPhoneAndTypeAndDeletedFalse(
            userRequest.phone(), uz.hesap.service.common.util.enums.UserType.CLIENT)
        .flatMap(
            exists -> {
              if (exists) {
                log.error("Phone or email already exists: {}", userRequest.phone());
                return Mono.error(
                    new AlreadyExistsException(
                        ALREADY_EXISTS_ERROR_CODE, "Phone or email already exists"));
              }
              UserEntity userEntity = UserMapper.INSTANCE.toUserEntity(userRequest);
              userEntity.setType(uz.hesap.service.common.util.enums.UserType.CLIENT);

              return userRepository
                  .save(userEntity)
                  .flatMap(
                      savedEntity ->
                          // Yangi mijoz — amoCRM'ga (RabbitMQ orqali) xabar berish (fire-and-forget).
                          userRegisteredNotifier
                              .notifyRegistered(savedEntity)
                              .thenReturn(UserMapper.INSTANCE.toUserResponse(savedEntity)));
            });
  }

  // Phone verify (OneID va boshqalar): token bilan, SMS kod yuborish
  public Mono<Void> phoneSendCode(
      final UserPrincipal principal, final PhoneSendCodeRequest request) {
    final String phone = normalizePhone(request.phone());
    final UUID userId = principal.user().id();

    return assertPhoneAvailableForUser(phone, userId).then(sendPhoneVerifyCode(phone));
  }

  // Kod tasdiqlangach token'dagi user telefonini yangilaydi yoki biriktiradi
  public Mono<PhoneConfirmResponse> phoneConfirm(
      final UserPrincipal principal, final PhoneConfirmRequest request) {
    final String phone = normalizePhone(request.phone());
    final String code = normalizeCode(request.code());
    final UUID userId = principal.user().id();

    var cached = cachedPhoneVerifyCode.getIfPresent(phone);
    if (cached == null) {
      return Mono.error(new InvalidArgumentException("Verification code expired or not found"));
    }
    if (!Objects.equals(cached, code)) {
      return Mono.error(new InvalidArgumentException("User code not match"));
    }

    cachedPhoneVerifyCode.invalidate(phone);

    return assertPhoneAvailableForUser(phone, userId)
        .then(
            userRepository
                .findByIdAndDeletedIsFalse(userId)
                .switchIfEmpty(Mono.error(new NotFoundException(USER_NOT_FOUND, "User not found")))
                .flatMap(user -> verifyPhoneBelongsToPinfl(user, phone, userId)))
        .map(user -> new PhoneConfirmResponse(user.getId(), phone));
  }

  // Telefonni o'rnatishdan oldin Turanix orqali tekshiramiz: shu MSISDN haqiqatan
  // ham foydalanuvchi PINFL'iga biriktirilganmi (resultCode==3000). Aks holda yoki
  // tekshiruv uvol bo'lsa — telefon o'rnatilmaydi (bloklash).
  private Mono<UserEntity> verifyPhoneBelongsToPinfl(
      final UserEntity user, final String phone, final UUID userId) {
    // Sozlama (DB) o'chiq bo'lsa — tekshiruvsiz to'g'ridan-to'g'ri o'rnatamiz.
    return systemSettingService
        .isTuranixPhoneCheckEnabled()
        .flatMap(
            enabled -> {
              if (!Boolean.TRUE.equals(enabled)) {
                user.setPhone(phone);
                return userRepository.save(user);
              }
              return runTuranixPhoneCheck(user, phone, userId);
            });
  }

  // Turanix orqali MSISDN↔PINFL tekshiruvi (sozlama yoniq bo'lganda).
  private Mono<UserEntity> runTuranixPhoneCheck(
      final UserEntity user, final String phone, final UUID userId) {
    final String pinfl = user.getIn();
    if (pinfl == null || pinfl.isBlank()) {
      return Mono.error(
          new InvalidArgumentException(
              "PINFL aniqlanmagan — telefon raqamni tasdiqlab bo'lmaydi"));
    }
    return integrationServiceClient
        .checkPassMsisdn(userId, phone, pinfl)
        .flatMap(
            result -> {
              if (result.resultCode() != null && result.resultCode() == 3000) {
                user.setPhone(phone);
                return userRepository.save(user);
              }
              final String reason =
                  result.description() != null && !result.description().isBlank()
                      ? result.description()
                      : "Bu telefon raqam sizning PINFL'ingizga biriktirilmagan";
              return Mono.error(new InvalidArgumentException(reason));
            })
        // Biznes xatosini (mos kelmadi/PINFL yo'q) o'tkazamiz; Turanix HTTP chaqiruvi
        // uvol bo'lsa — telefonni o'rnatmaymiz (xavfsiz), umumiy xabar qaytaramiz.
        .onErrorResume(
            e ->
                e instanceof InvalidArgumentException
                    ? Mono.error(e)
                    : Mono.error(
                        new InvalidArgumentException(
                            "Telefon raqamni tekshirib bo'lmadi, keyinroq urinib ko'ring")));
  }

  // AbleID orqali profilni tasdiqlaydi: attempt SUCCESS + pinfl mosligi -> isVerified=true.
  public Mono<UserResponse> verifyByAbleId(final UserPrincipal principal, final String attemptId) {
    final UUID userId = principal.user().id();
    if (attemptId == null || attemptId.isBlank()) {
      return Mono.error(new InvalidArgumentException("AbleID attemptId talab qilinadi"));
    }
    return integrationServiceClient
        .verifyAbleId(attemptId)
        .switchIfEmpty(
            Mono.error(new InvalidArgumentException("AbleID javobi bo'sh — tasdiqlab bo'lmadi")))
        .flatMap(
            res -> {
              if (!"SUCCESS".equalsIgnoreCase(res.status())) {
                return Mono.error(
                    new InvalidArgumentException(
                        "AbleID tasdiqlash yakunlanmagan, qayta urinib ko'ring"));
              }
              if (res.pinfl() == null || res.pinfl().isBlank()) {
                return Mono.error(
                    new InvalidArgumentException("AbleID'dan shaxs ma'lumoti olinmadi"));
              }
              return userRepository
                  .findByIdAndDeletedIsFalse(userId)
                  .switchIfEmpty(
                      Mono.error(new NotFoundException(USER_NOT_FOUND, "User not found")))
                  .flatMap(
                      user -> {
                        final String userIn = user.getIn();
                        if (userIn != null && !userIn.isBlank() && !userIn.equals(res.pinfl())) {
                          return Mono.error(
                              new ForbiddenException(
                                  "AbleID orqali tasdiqlangan shaxs hisobingizga mos kelmadi"));
                        }
                        if (userIn == null || userIn.isBlank()) {
                          user.setIn(res.pinfl());
                        }
                        user.setIsVerified(Boolean.TRUE);
                        return userRepository.save(user);
                      });
            })
        .flatMap(saved -> getMe(userId, principal.sessionId()));
  }

  // MyID (web OAuth) orqali profilni tasdiqlaydi: code → pinfl → user.isVerified=true.
  // pinfl user'ning mavjud pinfl'iga mos kelishi shart; pinfl yo'q bo'lsa biriktiriladi.
  public Mono<UserResponse> verifyByMyId(
      final UserPrincipal principal, final String code, final String platform) {
    final UUID userId = principal.user().id();
    return integrationServiceClient
        .verifyMyId(code, userId, platform)
        // MyID exchange bo'sh javob qaytarsa (token/users-me bo'sh) — jim o'tib ketmaymiz.
        .switchIfEmpty(
            Mono.error(
                new InvalidArgumentException("MyID javobi bo'sh — tasdiqlab bo'lmadi")))
        .flatMap(
            res -> {
              if (res.pinfl() == null || res.pinfl().isBlank()) {
                return Mono.error(
                    new InvalidArgumentException("MyID'dan shaxs ma'lumoti olinmadi"));
              }
              return userRepository
                  .findByIdAndDeletedIsFalse(userId)
                  .switchIfEmpty(
                      Mono.error(new NotFoundException(USER_NOT_FOUND, "User not found")))
                  .flatMap(
                      user -> {
                        final String userIn = user.getIn();
                        if (userIn != null && !userIn.isBlank() && !userIn.equals(res.pinfl())) {
                          return Mono.error(
                              new ForbiddenException(
                                  "MyID orqali tasdiqlangan shaxs hisobingizga mos kelmadi"));
                        }
                        if (userIn == null || userIn.isBlank()) {
                          user.setIn(res.pinfl());
                        }
                        user.setIsVerified(Boolean.TRUE);
                        // MyID'dan kelgan passport ma'lumotlarini user'ga yozamiz
                        // (bo'sh bo'lganlarini o'zgartirmaymiz; WEB oqimida null kelishi mumkin).
                        if (notBlank(res.passport())) user.setPassport(res.passport());
                        if (notBlank(res.issuedBy())) user.setPassportIssuedBy(res.issuedBy());
                        if (notBlank(res.issueDate())) user.setPassportIssueDate(res.issueDate());
                        if (notBlank(res.expiryDate())) user.setPassportExpiryDate(res.expiryDate());
                        if (notBlank(res.firstName())) user.setFirstName(res.firstName());
                        if (notBlank(res.lastName())) user.setLastName(res.lastName());
                        if (notBlank(res.middleName())) user.setMidName(res.middleName());
                        if (notBlank(res.birthDate())) user.setBirthday(res.birthDate());
                        if (notBlank(res.birthPlace())) user.setBirthPlace(res.birthPlace());
                        if (notBlank(res.nationality())) user.setNationality(res.nationality());
                        if (notBlank(res.citizenship())) user.setCitizenship(res.citizenship());
                        if (notBlank(res.address())) user.setAddress(res.address());
                        return userRepository.save(user);
                      });
            })
        // getMe FAQAT save muvaffaqiyatli bo'lgach ishlaydi (avval .then bo'sh holatda
        // ham getMe'ni chaqirib, verified=false qaytarardi).
        .flatMap(saved -> getMe(userId, principal.sessionId()));
  }

  private static boolean notBlank(String s) {
    return s != null && !s.isBlank();
  }

  // E-IMZO orqali yuridik shaxsni tasdiqlaydi: sertifikat STIR'i (1.2.860.3.16.1.1)
  // COMPANY user'ga mos kelsa va foydalanuvchi shu kompaniyaga bog'langan bo'lsa
  // → COMPANY.isVerified=true. Sessiya egasi (jismoniy shaxs) o'zgarmaydi.
  public Mono<UserResponse> verifyByEImzo(
      final UserPrincipal principal, final String pkcs7, final String ipAddress) {
    final UUID personId = principal.user().id();
    return integrationServiceClient
        .eImzoAuth(pkcs7, ipAddress)
        .flatMap(
            res -> {
              if (res.status() != 1) {
                return Mono.error(new InvalidArgumentException("E-IMZO sertifikati tasdiqlanmadi"));
              }
              final String tin =
                  res.subjectCertificateInfo() != null
                          && res.subjectCertificateInfo().subjectName() != null
                      ? res.subjectCertificateInfo().subjectName().get("1.2.860.3.16.1.1")
                      : null;
              if (tin == null || tin.isBlank()) {
                return Mono.error(
                    new InvalidArgumentException("Sertifikatda yuridik shaxs STIR'i topilmadi"));
              }
              return userRepository
                  .findFirstByTinAndTypeAndDeletedFalseOrderByCreatedDateAsc(
                      tin, uz.hesap.service.common.util.enums.UserType.COMPANY)
                  .switchIfEmpty(
                      Mono.error(
                          new NotFoundException(
                              USER_NOT_FOUND, "Bu STIR bo'yicha yuridik shaxs topilmadi")))
                  .flatMap(
                      company ->
                          staffRepository
                              .findFirstByUserIdAndCompanyIdAndDeletedFalse(personId, company.getId())
                              .switchIfEmpty(
                                  Mono.error(
                                      new ForbiddenException(
                                          "Bu yuridik shaxs hisobingizga bog'lanmagan")))
                              .flatMap(
                                  link -> {
                                    company.setIsVerified(Boolean.TRUE);
                                    return userRepository.save(company);
                                  }));
            })
        .then(getMe(personId, principal.sessionId()));
  }

  private Mono<Void> assertPhoneAvailableForUser(final String phone, final UUID userId) {
    return userRepository
        .findFirstByPhoneAndDeletedFalseOrderByCreatedDateAsc(phone)
        .flatMap(
            existing -> {
              if (!existing.getId().equals(userId)) {
                return Mono.error(
                    new AlreadyExistsException(
                        USER_PHONE_EXIST, "Phone already registered to another user"));
              }
              return Mono.empty();
            })
        .then();
  }

  private Mono<Void> sendPhoneVerifyCode(final String phone) {
    var cached = cachedPhoneVerifyCode.getIfPresent(phone);
    if (cached != null) {
      return notificationServiceClient
          .sendSms(phone, String.format(PHONE_VERIFY_SMS_TEMPLATE, cached))
          .then();
    }

    final var code = generateVerificationCode(phone);
    cachedPhoneVerifyCode.put(phone, code);
    return notificationServiceClient
        .sendSms(phone, String.format(PHONE_VERIFY_SMS_TEMPLATE, code))
        .doOnError(
            error -> {
              cachedPhoneVerifyCode.invalidate(phone);
              log.error("Failed to send phone verify SMS for phone: {}", phone, error);
            })
        .then();
  }

  // C2C: o'z akkauntini soft delete qiladi (deleted = true).
  public Mono<Void> deleteMe(final UUID userId) {
    return userRepository
        .findByIdAndDeletedIsFalse(userId)
        .flatMap(
            user -> {
              user.setDeleted(Boolean.TRUE);
              return userRepository.save(user);
            })
        .then();
  }

  public Mono<UserResponse> getMe(final UUID userId, final UUID sessionId) {
    log.debug("Me: {}", userId);
    return userRepository
        .findByIdAndDeletedIsFalse(userId)
        // User topilmasa shu yerda xato — session YO'Q/arxivlangan bo'lsa "User not found"
        // BERMASIN: qurilma konteksti (session'dan olinadi) ixtiyoriy, profil baribir qaytadi.
        .switchIfEmpty(Mono.error(new NotFoundException(USER_NOT_FOUND, "User not found")))
        .zipWith(
            sessionRepository
                .findById(sessionId)
                .map(SessionMapper.INSTANCE::toSessionResponse)
                .map(java.util.Optional::of)
                .defaultIfEmpty(java.util.Optional.empty()))
        .flatMap(
            tuple -> {
              var user = tuple.getT1();
              var device = tuple.getT2().orElse(null);

              if (user.getType() == UserType.ADMIN || user.getType() == UserType.SUPER_ADMIN) {
                return Mono.just(
                    UserMapper.INSTANCE.toUserResponse(user, device, user.getRole(), null));
              }
              if (user.getType() == UserType.CLIENT) {
                // Jismoniy shaxs aktiv (qabul qilingan) staff bog'lanishi orqali kompaniya
                // (COMPANY user) nomidan ish ko'radi. Bog'lanish yo'q bo'lsa — null company (C2C).
                return staffRepository
                    .findFirstByUserIdAndStatusAndDeletedFalseOrderByCreatedDateAsc(
                        userId, StaffStatus.ACCEPTED)
                    .flatMap(
                        staff ->
                            resolveCompanyResponse(staff.getCompanyId())
                                .map(
                                    company ->
                                        UserMapper.INSTANCE.toUserResponse(
                                            user, device, staffTypeToRole(staff.getType()), company)))
                    .defaultIfEmpty(
                        UserMapper.INSTANCE.toUserResponse(user, device, user.getRole(), null));
              }

              // COMPANY (o'zi tashkilot) va boshqa turlar — null company.
              return Mono.just(
                  UserMapper.INSTANCE.toUserResponse(user, device, user.getRole(), null));
            })
        .switchIfEmpty(Mono.error(new NotFoundException(USER_NOT_FOUND, "User not found")))
        .doOnNext(log::debug);
  }

  // companyId — COMPANY user (OneID yuridik login orqali yaratilgan) id'si;
  // uni CompanyResponse'ga aylantiradi.
  private Mono<CompanyResponse> resolveCompanyResponse(final UUID companyId) {
    return userRepository
        .findByIdAndDeletedIsFalse(companyId)
        .filter(u -> u.getType() == UserType.COMPANY)
        .map(UserService::toCompanyResponseFromUser);
  }

  // Staff roli → UserResponse uchun Role: OWNER→OWNER, MANAGER→WORKER.
  private static uz.hesap.service.common.util.enums.Role staffTypeToRole(
      final uz.hesap.service.main.domain.enums.StaffType type) {
    if (type == uz.hesap.service.main.domain.enums.StaffType.OWNER) {
      return uz.hesap.service.common.util.enums.Role.OWNER;
    }
    return uz.hesap.service.common.util.enums.Role.WORKER;
  }

  // COMPANY user (yuridik shaxs) → CompanyResponse.
  private static CompanyResponse toCompanyResponseFromUser(final UserEntity company) {
    return new CompanyResponse(
        company.getId(),
        CompanyType.LEGAL,
        company.getLegalName(),
        null,
        company.getTin(),
        Boolean.TRUE,
        company.getDeleted(),
        company.getCreatedDate(),
        company.getLastModifiedDate());
  }

  public Mono<UserResponse> updateUser(
      final UserPrincipal userPrincipal, final UUID id, final UserUpdateRequest userRequest) {
    log.debug("Update user: {}", id);
    return userRepository
        .findByIdAndDeletedIsFalse(id)
        .switchIfEmpty(Mono.error(new NotFoundException(USER_NOT_FOUND, "User not found")))
        .flatMap(
            existingUser -> {
              existingUser.setFirstName(userRequest.firstName());
              existingUser.setLastName(userRequest.lastName());
              return userRepository
                  .save(existingUser)
                  .map(UserMapper.INSTANCE::toUserResponse)
                  .doOnSuccess(
                      response -> {
                        log.debug("User updated: {}", response);
                      });
            });
  }

  public Mono<Boolean> deleteUser(final UserPrincipal userPrincipal, final UUID id) {
    log.debug("Delete user: {}", id);
    return userRepository
        .findByIdAndDeletedIsFalse(id)
        .switchIfEmpty(Mono.error(new NotFoundException(USER_NOT_FOUND, "User not found")))
        .flatMap(
            user -> {
              user.setDeleted(Boolean.TRUE);
              return userRepository.save(user).thenReturn(Boolean.TRUE);
            })
        .onErrorResume(
            error -> {
              log.error("Error occurred while deleting user: {}", error.getMessage());
              return Mono.just(Boolean.FALSE);
            });
  }

  public Mono<UserResponse> findByIdWithoutDeleted(UUID id) {
    log.debug("findByIdWithoutDeleted user By id: {}", id);
    return userRepository.findById(id).map(UserMapper.INSTANCE::toUserResponse);
  }

  public Mono<UserResponse> findById(UUID id) {
    log.debug("Find user By id: {}", id);
    return userRepository
        .findByIdAndDeletedIsFalse(id)
        .switchIfEmpty(
            Mono.error(new NotFoundException(USER_NOT_FOUND, "User not found with Id: " + id)))
        .map(UserMapper.INSTANCE::toUserResponse);
  }

  public Mono<UserResponse> findByIdWithDeleted(UUID id) {
    log.debug("Find user By id: {}", id);
    return userRepository
        .findById(id)
        .switchIfEmpty(
            Mono.error(new NotFoundException(USER_NOT_FOUND, "User not found with Id: " + id)))
        .map(UserMapper.INSTANCE::toUserResponse);
  }

  // Mijozlar (admin) ro'yxati — turi (CLIENT/COMPANY), MyID, shartnoma soni,
  // balans va oxirgi tashrif bo'yicha filtr + agregatlar (findByFilter/countByFilter).
  // Mijozlar yosh taqsimoti — oraliqda (createdFrom/createdTo) ro'yxatdan o'tganlar
  // bo'yicha, PINFL'dan SQL agregatsiya (butun populyatsiya, frontend namunasi emas).
  public Flux<AgeBucketCount> clientAgeDistribution(
      java.time.Instant createdFrom, java.time.Instant createdTo) {
    return userRepository.clientAgeDistribution(createdFrom, createdTo);
  }

  // Mijozlar jinsi taqsimoti — oraliqda, PINFL'dan SQL agregatsiya.
  public Flux<AgeBucketCount> clientGenderDistribution(
      java.time.Instant createdFrom, java.time.Instant createdTo) {
    return userRepository.clientGenderDistribution(createdFrom, createdTo);
  }

  public Mono<Page<AdminUserResponse>> getAllPage(
      final UserPrincipal userPrincipal,
      String search,
      UserType type,
      Boolean isVerified,
      Integer contractCountFrom,
      Integer contractCountTo,
      Double balanceFrom,
      Double balanceTo,
      java.time.Instant lastVisitFrom,
      java.time.Instant lastVisitTo,
      java.time.Instant createdFrom,
      java.time.Instant createdTo,
      Boolean hasPinfl,
      Pageable pageable) {
    log.debug("Get user page with search: '{}' type: {} pageable: {}", search, type, pageable);
    AdminUserFilter filter =
        new AdminUserFilter(
            null,
            search,
            null,
            type,
            isVerified,
            contractCountFrom,
            contractCountTo,
            balanceFrom,
            balanceTo,
            lastVisitFrom,
            lastVisitTo,
            createdFrom,
            createdTo,
            hasPinfl);
    Mono<List<AdminUserResponse>> rowsMono =
        userRepository
            .findByFilter(filter, pageable)
            .map(
                row ->
                    UserMapper.INSTANCE.toAdminUserResponse(
                        row.user(),
                        List.of(),
                        List.of(),
                        row.contractsCount(),
                        row.balance(),
                        row.lastVisitDate()))
            .collectList();
    Mono<Long> totalCountMono = userRepository.countByFilter(filter);
    return Mono.zip(rowsMono, totalCountMono)
        .map(tuple -> new PageImpl<>(tuple.getT1(), pageable, tuple.getT2()));
  }


  public Flux<UserBasicResponse> getBasicUsersIdIn(final List<UUID> idList) {
    return userRepository
        .findAllByIdIn(idList)
        .switchIfEmpty(Flux.empty())
        .onErrorResume(e -> Flux.empty())
        .map(UserMapper.INSTANCE::toUserBasicResponse);
  }

  public Flux<UserResponse> getUsersIdIn(final List<UUID> idList) {
    return userRepository
        .findAllByIdIn(idList)
        .switchIfEmpty(Flux.empty())
        .onErrorResume(e -> Flux.empty())
        .map(UserMapper.INSTANCE::toUserResponse);
  }

  public Mono<UserResponse> getUserByIn(String in) {
    // `in` bo'yicha — turdan qat'i nazar: CLIENT (pinfl=14) yoki COMPANY (in=STIR=9).
    // CLIENT-only filtr yuridik shaxs (legalName) party'larini topa olmas edi.
    // Faol yozuv topilmasa — o'chirilgan bo'lsa ham eng oxirgi yozuvga fallback,
    // shunda akkauntini o'chirgan shartnoma tomoni ham profil sahifasida ochiladi.
    return userRepository
        .findFirstByInAndDeletedFalseOrderByCreatedDateAsc(in)
        .switchIfEmpty(userRepository.findFirstByInOrderByCreatedDateDesc(in))
        .map(UserMapper.INSTANCE::toUserResponse);
  }

  // UUID id bo'yicha foydalanuvchi (profil sahifasi clients/:id uchun).
  // Faol yozuv topilmasa — o'chirilgan bo'lsa ham qaytaramiz (shartnoma tomoni
  // akkauntini o'chirgan bo'lsa ham profili ochilsin), aks holda 404.
  public Mono<UserResponse> getUserById(UUID id) {
    return userRepository
        .findByIdAndDeletedIsFalse(id)
        .switchIfEmpty(userRepository.findById(id))
        .switchIfEmpty(Mono.error(new NotFoundException(USER_NOT_FOUND, "User not found")))
        .map(UserMapper.INSTANCE::toUserResponse);
  }

  // C2C: o'z profilini tahrirlash (image, phone, secondPhone)
  public Mono<UserResponse> updateClientProfile(UUID userId, ClientProfileUpdateRequest request) {
    return userRepository
        .findByIdAndDeletedIsFalse(userId)
        .switchIfEmpty(Mono.error(new NotFoundException(USER_NOT_FOUND, "User not found")))
        .map(user -> UserMapper.INSTANCE.updateProfileFromRequest(user, request))
        .flatMap(userRepository::save)
        .map(UserMapper.INSTANCE::toUserResponse);
  }

  // Get OneID passport userInfo — endi integration servisidan keladi.
  // OneID passport yo'q bo'lsa integration 404 qaytaradi — buni 500 qilmay,
  // bo'sh (empty) qaytaramiz (passport ixtiyoriy: rasm/manzil bo'lmasligi mumkin).
  public Mono<OneIdPassportResponse> getPassportInfo(UUID userId) {
    return integrationServiceClient.getOneIdPassport(userId).onErrorResume(e -> Mono.empty());
  }

  // IN bo'yicha CLIENT topib, OneID passport ma'lumotlarini qaytarish.
  public Mono<OneIdPassportResponse> getPassportByIn(String in) {
    return getClientByIn(in).flatMap(user -> getPassportInfo(user.id()));
  }

  // PDF va boshqa servislar uchun: OneID + user (haqdor/qarzdor rekvizitlari).
  // COMPANY (yuridik shaxs) tomoni bo'lsa — kompaniyaning OWNER staff'idan
  // direktor F.I.SH ham to'ldiriladi (owner* maydonlar).
  public Mono<UserPassportBasicResponse> getPartyPassportBasic(UUID userId) {
    Mono<OneIdPassportResponse> passport =
        integrationServiceClient.getOneIdPassport(userId).onErrorResume(e -> Mono.empty());
    Mono<UserResponse> user =
        userRepository
            .findByIdAndDeletedIsFalse(userId)
            .map(UserMapper.INSTANCE::toUserResponse);

    return user.flatMap(
        u ->
            findOwner(u)
                .flatMap(
                    ownerOpt ->
                        passport
                            .map(p -> toPartyPassportBasic(userId, p, u, ownerOpt.orElse(null)))
                            .switchIfEmpty(
                                Mono.just(
                                    fromUserPassportBasic(userId, u, ownerOpt.orElse(null))))));
  }

  // COMPANY user'i uchun OWNER (direktor) staff'ini topib, tegishli user'ni yuklaydi.
  // Aks holda — bo'sh Optional. Xato bo'lsa ham xabar berilmaydi (direktor optional).
  private Mono<java.util.Optional<UserResponse>> findOwner(UserResponse user) {
    if (user.type() != UserType.COMPANY) {
      return Mono.just(java.util.Optional.empty());
    }
    return staffRepository
        .findFirstByCompanyIdAndTypeAndStatusAndDeletedFalse(
            user.id(),
            uz.hesap.service.main.domain.enums.StaffType.OWNER,
            uz.hesap.service.main.domain.enums.StaffStatus.ACCEPTED)
        .flatMap(staff -> userRepository.findByIdAndDeletedIsFalse(staff.getUserId()))
        .map(UserMapper.INSTANCE::toUserResponse)
        .map(java.util.Optional::of)
        .defaultIfEmpty(java.util.Optional.empty())
        .onErrorReturn(java.util.Optional.empty());
  }

  private UserPassportBasicResponse toPartyPassportBasic(
      UUID userId, OneIdPassportResponse passport, UserResponse user, UserResponse owner) {
    return new UserPassportBasicResponse(
        userId,
        firstNonBlank(passport.fullName(), buildFullName(user)),
        nullToEmpty(user.firstName()),
        nullToEmpty(user.lastName()),
        nullToEmpty(user.midName()),
        firstNonBlank(passport.document(), passport.pin(), user.document()),
        nullToEmpty(user.in()),
        nullToEmpty(user.phone()),
        nullToEmpty(passport.address()),
        nullToEmpty(user.legalName()),
        nullToEmpty(user.tin()),
        ownerFullName(owner),
        nullToEmpty(owner != null ? owner.firstName() : null),
        nullToEmpty(owner != null ? owner.lastName() : null),
        nullToEmpty(owner != null ? owner.midName() : null),
        user.type(),
        Boolean.TRUE.equals(user.verified()));
  }

  private UserPassportBasicResponse fromUserPassportBasic(
      UUID userId, UserResponse user, UserResponse owner) {
    return new UserPassportBasicResponse(
        userId,
        buildFullName(user),
        nullToEmpty(user.firstName()),
        nullToEmpty(user.lastName()),
        nullToEmpty(user.midName()),
        nullToEmpty(user.document()),
        nullToEmpty(user.in()),
        nullToEmpty(user.phone()),
        nullToEmpty(user.address()),
        nullToEmpty(user.legalName()),
        nullToEmpty(user.tin()),
        ownerFullName(owner),
        nullToEmpty(owner != null ? owner.firstName() : null),
        nullToEmpty(owner != null ? owner.lastName() : null),
        nullToEmpty(owner != null ? owner.midName() : null),
        user.type(),
        Boolean.TRUE.equals(user.verified()));
  }

  // OWNER F.I.SH (Familiya Ism Otchestvo, hujjatga qulay tartib). Owner null bo'lsa "".
  private static String ownerFullName(UserResponse owner) {
    if (owner == null) return "";
    String ln = nullToEmpty(owner.lastName());
    String fn = nullToEmpty(owner.firstName());
    String mn = nullToEmpty(owner.midName());
    String result = (ln + " " + fn + " " + mn).replaceAll("\\s+", " ").trim();
    return result;
  }

  // To'liq nom: yuridik shaxs (COMPANY) -> legalName, jismoniy -> F.I.O.
  private static String buildFullName(UserResponse user) {
    if (user.type() == UserType.COMPANY
        && user.legalName() != null
        && !user.legalName().isBlank()) {
      return user.legalName();
    }
    return Stream.of(user.lastName(), user.firstName(), user.midName())
        .filter(part -> part != null && !part.isBlank())
        .collect(Collectors.joining(" "));
  }

  private static String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value;
      }
    }
    return "";
  }

  private static String nullToEmpty(String value) {
    return value == null ? "" : value;
  }

  // Get user ID by IN (PINFL). `findFirstBy…OrderBy…` LIMIT 1 qo'shadi —
  // bir xil IN bilan duplicate CLIENT qatorlar uchun "returned non
  // unique result" 500 xatosini oldini oladi (eng eski yozuvni oladi).
  public Mono<UUID> getUserIdByIn(String in) {
    return userRepository
        .findFirstByInAndTypeAndDeletedFalseOrderByCreatedDateAsc(in, UserType.CLIENT)
        .map(uz.hesap.service.main.domain.UserEntity::getId)
        .switchIfEmpty(Mono.error(new NotFoundException(USER_NOT_FOUND, "User not found")));
  }

  // IN (PINFL) bo'yicha CLIENT user — to'liq UserResponse qaytaradi.
  // Mobile clientlar va Fuqaroni tekshirish sahifasi shu metoddan
  // foydalanadi. `findFirstBy…OrderBy…` LIMIT 1 — duplicate CLIENT
  // qatorlar uchun "returned non unique result" 500 xatosini oldini
  // oladi. Topilmasa NotFoundException → 404.
  public Mono<UserResponse> getClientByIn(String in) {
    // Avval jismoniy shaxs (CLIENT) PINFL (`in`) bo'yicha; topilmasa yuridik
    // shaxs (COMPANY) STIR (`tin`) bo'yicha qidiriladi — shartnoma tarafi yuridik
    // bo'lganda ham profil/info ochilishi uchun (aks holda 404 bo'lardi).
    return userRepository
        .findFirstByInAndTypeAndDeletedFalseOrderByCreatedDateAsc(in, UserType.CLIENT)
        .switchIfEmpty(
            userRepository.findFirstByTinAndTypeAndDeletedFalseOrderByCreatedDateAsc(
                in, UserType.COMPANY))
        .map(UserMapper.INSTANCE::toUserResponse)
        .switchIfEmpty(Mono.error(new NotFoundException(USER_NOT_FOUND, "User not found")));
  }

  private String generateVerificationCode(final String phone) {
    if (isDevelopment || isTestPhone(phone)) {
      return "13579";
    }
    return String.valueOf(10_000 + SECURE_RANDOM.nextInt(89_999));
  }

  private boolean isTestPhone(final String phone) {
    if (testPhones == null || testPhones.isBlank()) {
      return false;
    }
    for (String testPhone : testPhones.split(",")) {
      if (normalizePhone(testPhone.trim()).equals(phone)) {
        return true;
      }
    }
    return false;
  }

  private String normalizePhone(final String phone) {
    if (phone == null) {
      return "";
    }
    return phone.replace("+", "").replace(" ", "").replace("-", "");
  }

  private String normalizeCode(final String code) {
    return code == null ? null : code.trim();
  }
}
