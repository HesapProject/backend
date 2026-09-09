package uz.hesap.service.main.service;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.exception.BadRequestException;
import uz.hesap.service.common.exception.NotFoundException;
import uz.hesap.service.common.exception.handler.EImzoError;
import uz.hesap.service.common.util.enums.UserType;
import uz.hesap.service.main.domain.SessionEntity;
import uz.hesap.service.main.feign.IntegrationServiceClient;
import uz.hesap.service.main.model.mapper.SessionMapper;
import uz.hesap.service.main.model.request.EImzoLoginRequest;
import uz.hesap.service.main.model.response.JwtTokenResponse;
import uz.hesap.service.main.repository.SessionRepository;
import uz.hesap.service.main.repository.UserRepository;

// E-IMZO bilan barcha HTTP integration servisida. main-service faqat
// User/Device/Session/JWT orchestratori — sertifikat datasini Feign'dan oladi.
@Service
@Log4j2
@RequiredArgsConstructor
public class EImzoService {

  private final IntegrationServiceClient integrationClient;
  private final UserRepository userRepository;
  private final SessionRepository sessionRepository;
  private final JwtService jwtService;
  private final SessionService sessionService;

  public Mono<JwtTokenResponse> auth(EImzoLoginRequest request, ServerHttpRequest req) {
    log.info("E-IMZO auth request received. uuid={}", request.uuid());
    String ipAddress = extractIp(req);
    return Mono.fromCallable(
            () -> {
              if (request.pkcs7() == null || request.pkcs7().isEmpty()) {
                log.error("PKCS7 is empty");
                throw new IllegalArgumentException("PKCS#7 invalid");
              }
              return request;
            })
        .flatMap(r -> integrationClient.eImzoAuth(r.pkcs7(), ipAddress))
        .flatMap(
            response -> {
              if (response.status() != 1) {
                String messageByCode = EImzoError.getMessageByCode(response.status());
                return Mono.error(new BadRequestException(messageByCode));
              }
              String in =
                  response.subjectCertificateInfo().subjectName().get("1.2.860.3.16.1.2");
              if (in == null) {
                return Mono.error(new RuntimeException("PINFL not found in  certificate"));
              }
              return processUserLogin(in, request);
            })
        .onErrorResume(
            e -> {
              log.error("Error during e-imzo auth", e);
              return Mono.error(e);
            });
  }

  private Mono<JwtTokenResponse> processUserLogin(String in, EImzoLoginRequest request) {
    return userRepository
        .findByInAndTypeAndDeletedFalse(in, UserType.CLIENT)
        .switchIfEmpty(Mono.error(new NotFoundException("User not found ")))
        .flatMap(
            user -> {
              SessionEntity session = SessionMapper.INSTANCE.toSession(request);
              session.setUserId(user.getId());
              return sessionService.openSession(session);
            })
        .map(
            session ->
                new JwtTokenResponse(
                    jwtService.generateToken(
                        session.getUserId(), session.getId(), "web-app")));
  }

  private String extractIp(ServerHttpRequest request) {
    String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isBlank()) {
      return xForwardedFor.split(",")[0].trim();
    }

    String xRealIp = request.getHeaders().getFirst("X-Real-IP");
    if (xRealIp != null && !xRealIp.isBlank()) {
      return xRealIp;
    }

    String ip =
        Optional.ofNullable(request.getRemoteAddress())
            .map(InetSocketAddress::getAddress)
            .map(InetAddress::getHostAddress)
            .orElse("127.0.0.1");

    if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
      return "127.0.0.1";
    }

    return ip;
  }
}
