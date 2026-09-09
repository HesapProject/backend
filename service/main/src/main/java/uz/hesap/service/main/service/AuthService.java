package uz.hesap.service.main.service;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.exception.UnauthorizedException;
import uz.hesap.service.common.util.enums.UserType;
import uz.hesap.service.main.domain.UserEntity;
import uz.hesap.service.main.model.request.AdminAuthRequest;
import uz.hesap.service.main.model.response.AdminAuthResponse;
import uz.hesap.service.main.repository.UserRepository;

// Admin / super_admin login (login + parol). User'dan alohida ajratilgan auth logikasi.
@Service
@RequiredArgsConstructor
@Log4j2
public class AuthService {

  private static final String ISSUER = "hesap-admin";

  private final UserRepository userRepository;
  private final JwtService jwtService;

  public Mono<AdminAuthResponse> login(final AdminAuthRequest request) {
    log.debug("Admin auth attempt for login={}", request.login());
    return userRepository
        .findByLoginAndDeletedFalse(request.login())
        .switchIfEmpty(Mono.error(new UnauthorizedException("Login yoki parol noto'g'ri")))
        .handle(
            (user, sink) -> {
              if (user.getType() != UserType.ADMIN && user.getType() != UserType.SUPER_ADMIN) {
                sink.error(new UnauthorizedException("Login yoki parol noto'g'ri"));
                return;
              }
              if (user.getPassword() == null
                  || !BCrypt.checkpw(request.password(), user.getPassword())) {
                sink.error(new UnauthorizedException("Login yoki parol noto'g'ri"));
                return;
              }
              sink.next(user);
            })
        .cast(UserEntity.class)
        .map(
            user -> {
              String token =
                  jwtService.generateToken(
                      Map.of("adminId", user.getId(), "type", user.getType().name()),
                      user.getId().toString(),
                      ISSUER);
              String fn = user.getFirstName() == null ? "" : user.getFirstName();
              String ln = user.getLastName() == null ? "" : user.getLastName();
              return new AdminAuthResponse(
                  token, user.getId(), user.getLogin(), user.getType(), (fn + " " + ln).trim());
            });
  }
}
