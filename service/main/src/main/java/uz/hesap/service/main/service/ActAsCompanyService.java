package uz.hesap.service.main.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.exception.ForbiddenException;
import uz.hesap.service.common.exception.NotFoundException;
import uz.hesap.service.common.util.UserPrincipal;
import uz.hesap.service.common.util.enums.UserType;
import uz.hesap.service.main.domain.SessionEntity;
import uz.hesap.service.main.domain.enums.StaffStatus;
import uz.hesap.service.main.model.response.JwtTokenResponse;
import uz.hesap.service.main.repository.SessionRepository;
import uz.hesap.service.main.repository.StaffRepository;
import uz.hesap.service.main.repository.UserRepository;

// Act-as-company: person token bilan companyId beriladi -> shu company nomidan
// ishlovchi token (userId=companyId) qaytadi. Bu token bilan document va h.k.
// company STIR bo'yicha scope qiladi (UserResponse.identifier()=STIR). Eski
// CompanyTokenService o'rnini bosadi, lekin a'zolikni `staff` (ACCEPTED) bo'yicha
// tekshiradi (company/user_company teardown'idan keyin).
@Log4j2
@Service
@RequiredArgsConstructor
public class ActAsCompanyService {

  private final StaffRepository staffRepository;
  private final SessionRepository sessionRepository;
  private final UserRepository userRepository;
  private final JwtService jwtService;

  public Mono<JwtTokenResponse> issue(final UserPrincipal principal, final UUID companyId) {
    final UUID personId = principal.user().id();
    log.debug("act-as-company requested: person={}, company={}", personId, companyId);

    return staffRepository
        // A'zolik: person shu company'ga ACCEPTED holatda biriktirilganmi (har qanday type).
        .findFirstByUserIdAndCompanyIdAndDeletedFalse(personId, companyId)
        .filter(staff -> staff.getStatus() == StaffStatus.ACCEPTED)
        .switchIfEmpty(
            Mono.error(
                new ForbiddenException("User is not an accepted staff of this company")))
        // companyId yaroqli COMPANY user'ga ishora qilishini tasdiqlaymiz — aks holda
        // userId=companyId sessiya ORPHAN bo'lib, token bilan /me, /document 401/500
        // berardi (sessiya bor, user yo'q). Yo'q bo'lsa shu yerda aniq xato qaytaramiz.
        .flatMap(
            staff ->
                userRepository
                    .findByIdAndDeletedIsFalse(companyId)
                    .filter(u -> u.getType() == UserType.COMPANY)
                    .switchIfEmpty(
                        Mono.error(new NotFoundException("Company user not found")))
                    .flatMap(company -> createCompanySession(personId, companyId)))
        .map(
            session ->
                new JwtTokenResponse(
                    jwtService.generateLongLivedToken(
                        companyId, session.getId(), "company")));
  }

  // Person+company uchun barqaror uuid bilan eski act-as sessiyani tozalab,
  // yangisini yaratadi (userId=companyId -> convertSessionToken validatsiyasi shu
  // bo'yicha ishlaydi, principal=company bo'ladi).
  private Mono<SessionEntity> createCompanySession(final UUID personId, final UUID companyId) {
    final String uuid = personId + "_" + companyId + "_actas";
    SessionEntity session = new SessionEntity();
    session.setUserId(companyId);
    session.setUuid(uuid);
    return sessionRepository.deleteByUuid(uuid).then(sessionRepository.save(session));
  }
}
