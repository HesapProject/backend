package uz.hesap.service.main.service;

import static uz.hesap.service.common.exception.handler.ErrorCode.*;

import com.github.benmanes.caffeine.cache.Cache;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.exception.AlreadyExistsException;
import uz.hesap.service.common.exception.BadRequestException;
import uz.hesap.service.common.exception.InvalidArgumentException;
import uz.hesap.service.common.exception.NotFoundException;
import uz.hesap.service.common.util.enums.UserType;
import uz.hesap.service.main.domain.SessionEntity;
import uz.hesap.service.main.domain.UserEntity;
import uz.hesap.service.main.feign.NotificationServiceClient;
import uz.hesap.service.main.model.mapper.SessionMapper;
import uz.hesap.service.main.model.ClientCacheModel;
import uz.hesap.service.main.model.request.CreateUserRequest;
import uz.hesap.service.main.model.request.LoginRequest;
import uz.hesap.service.main.model.request.SendCodeRequest;
import uz.hesap.service.main.model.request.VerifyCodeRequest;
import uz.hesap.service.main.model.response.CodeResponse;
import uz.hesap.service.main.model.response.JwtTokenResponse;
import uz.hesap.service.main.model.response.VerifySessionResponse;
import uz.hesap.service.main.model.response.VerifyTokenResponse;
import uz.hesap.service.main.repository.SessionRepository;
import uz.hesap.service.main.repository.UserRepository;

@Log4j2
@Service
public class ClientAuthService {

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  private static final String SMS_TEMPLATE =
      "Hesap.uz ilovasiga ro'yxatdan o'tish uchun tasdiqlash kod: %s";

  private final UserRepository userRepository;
  private final SessionRepository sessionRepository;
  private final SessionService sessionService;
  private final JwtService jwtService;
  private final NotificationServiceClient notificationServiceClient;
  private final Cache<String, ClientCacheModel> cachedClientAuth;
  private final UserRegisteredNotifier userRegisteredNotifier;

  @Value("${application.development:false}")
  private boolean isDevelopment;

  @Value("${application.test-phones:}")
  private String testPhones;

  public ClientAuthService(
      final UserRepository userRepository,
      final SessionRepository sessionRepository,
      final SessionService sessionService,
      final JwtService jwtService,
      final NotificationServiceClient notificationServiceClient,
      final Cache<String, ClientCacheModel> cachedClientAuth,
      final UserRegisteredNotifier userRegisteredNotifier) {
    this.userRepository = userRepository;
    this.sessionRepository = sessionRepository;
    this.sessionService = sessionService;
    this.jwtService = jwtService;
    this.notificationServiceClient = notificationServiceClient;
    this.cachedClientAuth = cachedClientAuth;
    this.userRegisteredNotifier = userRegisteredNotifier;
  }

  public Mono<CodeResponse> sendCode(final SendCodeRequest request) {
    log.debug("Send code request: {}", request);

    final String phone = normalizePhone(request.phone());

    // Validate action
    if (!SendCodeRequest.ACTION_CREATE.equals(request.action())
        && !SendCodeRequest.ACTION_RECOVERY.equals(request.action())) {
      return Mono.error(new InvalidArgumentException("action should be 'create' or 'recovery'"));
    }

    // Check user exists based on action
    return checkUserForAction(phone, request.action())
        .flatMap(
            valid -> {
              ClientCacheModel cache = cachedClientAuth.getIfPresent(phone);
              if (cache != null)
                return Mono.just(new CodeResponse(cache.phone(), cache.sendTime(), 120));

              // Generate verification code
              final String code = generateCode(phone);

              // Store in cache
              cachedClientAuth.put(
                  phone,
                  new ClientCacheModel(
                      request.firstName(),
                      request.lastName(),
                      phone,
                      code,
                      request.action(),
                      Instant.now()));

              // Send SMS via notification service
              return notificationServiceClient
                  .sendSms(phone, String.format(SMS_TEMPLATE, code))
                  .thenReturn(new CodeResponse(phone, Instant.now(), 120));
            });
  }

  public Mono<VerifyTokenResponse> verifyCode(final VerifyCodeRequest request) {
    log.debug("Verify code request for phone: {}", request.phone());
    final String phone = normalizePhone(request.phone());

    final ClientCacheModel cached = cachedClientAuth.getIfPresent(phone);
    if (cached == null) {
      log.error("No cached data found for phone: {}", request.phone());
      return Mono.error(new InvalidArgumentException(TIME_EXPIRED, "Code expired or not found"));
    }

    if (!cached.code().equals(request.code())) {
      log.error("Invalid code for phone: {}", request.phone());
      return Mono.error(new InvalidArgumentException(INVALID_CODE, "Invalid verification code"));
    }

    // Check if user already exists
    return userRepository
        .existsByPhoneAndTypeAndDeletedFalse(phone, UserType.CLIENT)
        .flatMap(
            exists -> {
              if (exists) {
                return Mono.error(
                    new BadRequestException(ALREADY_EXISTS_ERROR_CODE, "User already exists"));
              }
              // Create a temporary verify device with phone stored
              return createVerifyDevice(phone);
            })
        .map(
            device -> {
              // Generate verify token for sign-up
              String verifyToken = jwtService.generateVerifyToken(device.getId(), phone);
              log.info("Generated verify token for phone: {}", phone);
              // Invalidate cache after successful verification
              cachedClientAuth.invalidate(phone);
              return new VerifyTokenResponse(verifyToken);
            });
  }

  private Mono<SessionEntity> createVerifyDevice(String phone) {
    // Verify (ro'yxatdan o'tish) sessiyasi — user hali yo'q (user_id=null). Telefon bo'yicha
    // mavjudini qayta ishlatamiz, bo'lmasa yangi yaratamiz.
    return sessionRepository
        .findByPhoneAndIsVerifyDeviceTrue(phone)
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  SessionEntity verifySession = new SessionEntity();
                  verifySession.setUuid(UUID.randomUUID().toString());
                  verifySession.setPhone(phone);
                  verifySession.setIsVerifyDevice(Boolean.TRUE);
                  return sessionRepository.save(verifySession);
                }));
  }

  public Mono<JwtTokenResponse> createUserWithVerifyToken(
      final VerifySessionResponse verifyDevice, final CreateUserRequest request) {
    log.debug("Create user request for phone: {}", verifyDevice.phone());

    final String phone = verifyDevice.phone();

    // Check if user already exists (double check)
    return userRepository
        .existsByPhoneAndTypeAndDeletedFalse(phone, UserType.CLIENT)
        .flatMap(
            exists -> {
              if (exists) {
                return Mono.error(
                    new BadRequestException(ALREADY_EXISTS_ERROR_CODE, "User already exists"));
              }
              // Create new user
              UserEntity newUser = new UserEntity();
              newUser.setFirstName(request.firstName());
              newUser.setLastName(request.lastName());
              newUser.setUsername(request.username());
              newUser.setPhone(phone);
              newUser.setType(UserType.CLIENT);
              // Yangi mijoz — amoCRM'ga (RabbitMQ orqali) xabar berish (fire-and-forget).
              return userRepository
                  .save(newUser)
                  .flatMap(
                      saved ->
                          userRegisteredNotifier.notifyRegistered(saved).thenReturn(saved));
            })
        .flatMap(
            user -> {
              // Verify sessiyasini o'chiramiz — endi to'liq sessiya yaratiladi.
              return sessionRepository.deleteById(verifyDevice.id()).then(Mono.just(user));
            })
        .flatMap(
            user -> {
              // Create a new real device and session
              LoginRequest loginRequest =
                  new LoginRequest(
                      phone,
                      null,
                      verifyDevice.uuid(),
                      verifyDevice.osVersion(),
                      verifyDevice.os(),
                      verifyDevice.model(),
                      verifyDevice.brand(),
                      verifyDevice.type(),
                      verifyDevice.device(),
                      verifyDevice.fcmToken());
              return createNewSession(user, loginRequest);
            });
  }

  public Mono<Boolean> checkUsernameExists(final String username) {
    return userRepository.existsByUsernameAndTypeAndDeletedFalse(
        username.toLowerCase(), UserType.CLIENT);
  }

  public Mono<Boolean> checkPhoneExists(final String phone) {
    return userRepository.existsByPhoneAndTypeAndDeletedFalse(phone, UserType.CLIENT);
  }

  // ============ Private Helper Methods ============

  private Mono<Boolean> checkUserForAction(final String phone, final String action) {
    if (SendCodeRequest.ACTION_RECOVERY.equals(action)) {
      // For recovery, user must exist
      return userRepository
          .existsByPhoneAndTypeAndDeletedFalse(phone, UserType.CLIENT)
          .flatMap(
              exists -> {
                if (!exists) {
                  return Mono.error(new NotFoundException(USER_NOT_FOUND, "User not found"));
                }
                return Mono.just(Boolean.TRUE);
              });
    } else if (SendCodeRequest.ACTION_CREATE.equals(action)) {
      // For create, user must NOT exist
      return userRepository
          .existsByPhoneAndTypeAndDeletedFalse(phone, UserType.CLIENT)
          .flatMap(
              exists -> {
                if (exists) {
                  return Mono.error(
                      new AlreadyExistsException(
                          ALREADY_EXISTS_ERROR_CODE, "Phone already registered"));
                }
                return Mono.just(Boolean.TRUE);
              });
    }
    return Mono.just(Boolean.TRUE);
  }

  private String generateCode(final String phone) {
    // Check if it's a test phone or development mode
    if (isDevelopment || isTestPhone(phone)) {
      return "13579"; // Fixed code for testing
    }
    return String.valueOf(10_000 + SECURE_RANDOM.nextInt(89_999));
  }

  private boolean isTestPhone(final String phone) {
    if (testPhones == null || testPhones.isBlank()) {
      return false;
    }
    String normalizedPhone = normalizePhone(phone);
    for (String testPhone : testPhones.split(",")) {
      if (normalizePhone(testPhone.trim()).equals(normalizedPhone)) {
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

  private Mono<JwtTokenResponse> createNewSession(UserEntity user, LoginRequest request) {
    SessionEntity session = SessionMapper.INSTANCE.toSession(request);
    session.setUserId(user.getId());
    return sessionService
        .openSession(session)
        .map(
            s ->
                new JwtTokenResponse(
                    jwtService.generateToken(s.getUserId(), s.getId(), "web-app")));
  }
}
