package uz.hesap.service.main.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.exception.NotFoundException;
import uz.hesap.service.common.exception.handler.ErrorCode;
import uz.hesap.service.common.util.SessionResponse;
import uz.hesap.service.common.util.UserPrincipal;
import uz.hesap.service.main.domain.SessionEntity;
import uz.hesap.service.main.model.mapper.SessionMapper;
import uz.hesap.service.main.repository.SessionRepository;
import uz.hesap.service.main.repository.UserRepository;

// Sessiya (qurilma) boshqaruvi — barcha ma'lumot session jadvalida.
@Log4j2
@Service
@RequiredArgsConstructor
public class SessionService {
  private final SessionRepository sessionRepository;
  private final UserRepository userRepository;

  public Flux<String> findUserFirebaseTokens(final UUID userId) {
    log.debug("Find user fcm token By Id: {}", userId);
    return sessionRepository.findFcmTokensByUserId(userId);
  }

  // Login: shu qurilmadagi (uuid) eski sessiyalarni tozalab, yangi sessiya yaratadi.
  public Mono<SessionEntity> openSession(final SessionEntity session) {
    return sessionRepository
        .deleteByUuid(session.getUuid())
        .then(sessionRepository.save(session));
  }

  public Mono<Void> logout(final UserPrincipal userPrincipal) {
    log.debug("Logout requested for user [{}]", userPrincipal);
    final UUID userId = userPrincipal.user().id();
    return userRepository
        .findByIdAndDeletedIsFalse(userId)
        .switchIfEmpty(
            Mono.error(new NotFoundException(ErrorCode.USER_NOT_FOUND, "User not found")))
        .flatMap(user -> proceedLogout(userPrincipal))
        .doOnSuccess(v -> log.info("User [{}] logged out successfully", userId))
        .doOnError(e -> log.error("Failed to logout user [{}]: {}", userId, e.getMessage()));
  }

  private Mono<Void> proceedLogout(UserPrincipal userPrincipal) {
    // Joriy sessiya orqali qurilma uuid'sini topib, shu qurilmadagi BARCHA sessiyalarni
    // arxivlaymiz (person + company token). uuid yo'q bo'lsa faqat shu sessiya.
    final UUID sessionId = userPrincipal.sessionId();
    return sessionRepository
        .findById(sessionId)
        .flatMap(
            session ->
                session.getUuid() != null
                    ? sessionRepository.archiveByUuid(session.getUuid())
                    : sessionRepository.archiveById(sessionId))
        .switchIfEmpty(sessionRepository.archiveById(sessionId))
        .doOnError(
            error ->
                log.error(
                    "Error during logout for user [{}]: {}",
                    userPrincipal,
                    error.getMessage(),
                    error))
        .then();
  }

  public Flux<SessionResponse> findSessions(final UUID userId) {
    log.debug("Find sessions {}", userId);
    return sessionRepository
        .findAllByUserIdAndArchivedFalse(userId)
        .map(SessionMapper.INSTANCE::toSessionResponse);
  }

  // Admin (Control) — foydalanuvchining BARCHA sessiyalari (aktiv + arxiv).
  public Flux<uz.hesap.service.main.model.response.SessionAdminResponse> findAllForAdmin(
      final UUID userId) {
    log.debug("Admin find all sessions {}", userId);
    return sessionRepository
        .findAllByUserId(userId)
        .map(SessionMapper.INSTANCE::toSessionAdminResponse);
  }

  // Foydalanuvchi o'z sessiyasini tugatadi (arxivlanadi) — faqat o'ziga tegishli.
  public Mono<Void> killSession(final UserPrincipal userPrincipal, final UUID sessionId) {
    return sessionRepository.archiveByIdAndUserId(sessionId, userPrincipal.user().id()).then();
  }

  // Admin istalgan sessiyani arxivlaydi — userId tekshirilmaydi.
  public Mono<Void> killSessionByAdmin(final UUID sessionId) {
    return sessionRepository.archiveById(sessionId).then();
  }
}
