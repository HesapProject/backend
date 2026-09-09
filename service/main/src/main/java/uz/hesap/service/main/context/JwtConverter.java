package uz.hesap.service.main.context;

import io.jsonwebtoken.Claims;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.UserPrincipal;
import uz.hesap.service.common.util.UserResponse;
import uz.hesap.service.common.util.enums.Role;
import uz.hesap.service.common.util.enums.UserType;
import uz.hesap.service.main.model.mapper.SessionMapper;
import uz.hesap.service.main.repository.SessionRepository;
import uz.hesap.service.main.repository.UserRepository;
import uz.hesap.service.main.service.JwtService;
import uz.hesap.service.main.service.UserService;

public class JwtConverter implements Function<ServerWebExchange, Mono<Authentication>> {
  private final JwtService jwtService;
  private final UserService userService;
  private final SessionRepository sessionRepository;
  private final UserRepository userRepository;

  private static final Set<String> VERIFY_TOKEN_ALLOWED_PATHS = Set.of("/v1/auth/verify");

  public JwtConverter(
      JwtService jwtService,
      UserService userService,
      SessionRepository sessionRepository,
      UserRepository userRepository) {
    this.jwtService = jwtService;
    this.userService = userService;
    this.sessionRepository = sessionRepository;
    this.userRepository = userRepository;
  }

  @Override
  public Mono<Authentication> apply(ServerWebExchange exchange) {
    String requestPath = exchange.getRequest().getPath().value();

    return Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
        .filter(authHeader -> authHeader.startsWith("Bearer "))
        .flatMap(
            authHeader -> {
              String authToken = authHeader.substring(7);
              return convert(authToken, requestPath);
            });
  }

  private Mono<Authentication> convert(String authToken, String requestPath) {
    Claims allClaims;
    try {
      allClaims = jwtService.getAllClaims(authToken);
    } catch (Exception e) {
      return Mono.error(new JwtAuthenticationException(e.getMessage(), HttpStatus.UNAUTHORIZED));
    }

    // Admin token (control panel'dan) — alohida claim'lar:
    // {adminId, type:"ADMIN"|"SUPER_ADMIN"}. session/device tegishli emas.
    String tokenType = allClaims.get("type", String.class);
    if ("ADMIN".equals(tokenType) || "SUPER_ADMIN".equals(tokenType)) {
      return convertAdminToken(allClaims, authToken);
    }

    // Check if this is a verify token (for sign-up flow)
    if (jwtService.isVerifyToken(allClaims)) {
      // Verify token can ONLY be used for allowed paths
      if (!isPathAllowedForVerifyToken(requestPath)) {
        return Mono.error(
            new JwtAuthenticationException(
                "Verify token can only be used for sign-up", HttpStatus.UNAUTHORIZED));
      }
      return convertVerifyToken(allClaims, authToken);
    }

    // Regular session token - NOT allowed for verify-only endpoints
    if (isPathAllowedForVerifyToken(requestPath)) {
      // If trying to access sign-up with a regular session token, that's not allowed
      return Mono.error(
          new JwtAuthenticationException(
              "Sign-up requires a verify token", HttpStatus.UNAUTHORIZED));
    }

    // Regular session token flow
    return convertSessionToken(allClaims, authToken);
  }

  private boolean isPathAllowedForVerifyToken(String requestPath) {
    return VERIFY_TOKEN_ALLOWED_PATHS.contains(requestPath);
  }

  private Mono<Authentication> convertVerifyToken(Claims claims, String authToken) {
    UUID sessionId = jwtService.extractSessionIdFromVerifyToken(claims);

    return sessionRepository
        .findByIdAndIsVerifyDeviceTrue(sessionId)
        .switchIfEmpty(
            Mono.error(
                new JwtAuthenticationException(
                    "Invalid or expired verify token", HttpStatus.UNAUTHORIZED)))
        .map(
            session -> {
              var verifySessionResponse = SessionMapper.INSTANCE.toVerifySessionResponse(session);
              return new VerifyTokenAuthenticationToken(verifySessionResponse, authToken);
            });
  }

  // Admin tokenni session lookup'siz tan oladi va sintez qilingan
  // UserPrincipal qaytaradi. Boshqa servicelar `/me` chaqirig'ida
  // shu principal'ni qabul qiladi (UserController.getMe admin holda
  // principal.user()'ni to'g'ridan-to'g'ri qaytaradi).
  private Mono<Authentication> convertAdminToken(Claims claims, String authToken) {
    final UUID adminId;
    try {
      adminId = UUID.fromString(claims.get("adminId", String.class));
    } catch (Exception e) {
      return Mono.error(
          new JwtAuthenticationException("Admin token noto'g'ri", HttpStatus.UNAUTHORIZED));
    }

    return userRepository
        .findByIdAndDeletedIsFalse(adminId)
        .switchIfEmpty(
            Mono.error(new JwtAuthenticationException("Admin topilmadi", HttpStatus.UNAUTHORIZED)))
        .handle(
            (user, sink) -> {
              if (user.getType() != UserType.ADMIN && user.getType() != UserType.SUPER_ADMIN) {
                sink.error(
                    new JwtAuthenticationException("Admin emas", HttpStatus.UNAUTHORIZED));
                return;
              }
              UserResponse synthUser =
                  UserResponse.builder()
                      .id(user.getId())
                      .firstName(user.getFirstName())
                      .lastName(user.getLastName())
                      .username(user.getLogin())
                      .type(user.getType())
                      .role(user.getRole() != null ? user.getRole() : Role.ADMIN)
                      .build();
              sink.next(
                  new CurrentUserAuthenticationToken(
                      new UserPrincipal(synthUser, authToken, null)));
            });
  }

  private Mono<Authentication> convertSessionToken(Claims claims, String authToken) {
    return Mono.just(claims)
        .flatMap(
            c -> {
              final var userId = UUID.fromString(c.get("userId", String.class));
              final var sessionId = UUID.fromString(c.get("sessionId", String.class));
              return sessionRepository.findByIdAndUserId(sessionId, userId);
            })
        .switchIfEmpty(
            Mono.error(
                new JwtAuthenticationException("Wrong jwt credentials", HttpStatus.UNAUTHORIZED)))
        .flatMap(
            session ->
                userService.getMe(session.getUserId(), session.getId()).zipWith(Mono.just(session)))
        // Sessiya bor, lekin user qatori yo'q (o'chirilgan/yo'qolgan/orphan) -> getMe
        // "User not found" beradi. Buni 500 (butun ilova lockout) emas, 401 qilamiz —
        // frontend logout qilib qayta OneID login qiladi, yangi yaroqli sessiya oladi.
        .onErrorMap(
            uz.hesap.service.common.exception.NotFoundException.class,
            e ->
                new JwtAuthenticationException(
                    "Session user not found", HttpStatus.UNAUTHORIZED))
        .map(
            tuple ->
                new CurrentUserAuthenticationToken(
                    new UserPrincipal(
                        tuple.getT1(), authToken, tuple.getT2().getId())));
  }
}
