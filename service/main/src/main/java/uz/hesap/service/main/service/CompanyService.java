package uz.hesap.service.main.service;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.CompanyBasicResponse;
import uz.hesap.service.common.util.CompanyWithOwner;
import uz.hesap.service.common.util.UserPrincipal;
import uz.hesap.service.common.util.enums.CompanyType;
import uz.hesap.service.common.util.enums.UserType;
import uz.hesap.service.main.domain.enums.StaffStatus;
import uz.hesap.service.main.domain.enums.StaffType;
import uz.hesap.service.main.feign.IntegrationServiceClient;
import uz.hesap.service.main.repository.StaffRepository;
import uz.hesap.service.main.repository.UserRepository;

// Kompaniya endi alohida jadval emas — COMPANY typli UserEntity. Bog'lanish `staff` orqali.
// Bu servis cross-service (LocalController) company so'rovlarini COMPANY user'lardan beradi.
@Log4j2
@Service
@RequiredArgsConstructor
public class CompanyService {
  private final UserRepository userRepository;
  private final StaffRepository staffRepository;
  private final IntegrationServiceClient integrationClient;

  // sso.egov.uz SSO URL'ni integration servisidan oladi (creds DB'da, integration tomonida).
  // PERSONAL — jismoniy shaxs SSO orqali yuridik shaxs ma'lumotini olishi (scope=legal_info).
  // LEGAL    — yuridik shaxs PKCS sertifikat orqali (auth_methods=LEPKCSMETHOD).
  public Mono<String> getUrl(
      UserPrincipal userPrincipal,
      final CompanyType type,
      final String customName,
      final String redirectUrl) {
    log.debug("getUrl type [{}], customName [{}], redirectUrl [{}]", type, customName, redirectUrl);
    if (type == CompanyType.PERSONAL) {
      return integrationClient.getOneIdLoginUrl(redirectUrl, "legal_info", customName, null);
    }
    return integrationClient.getOneIdLoginUrl(redirectUrl, "user_info", customName, "LEPKCSMETHOD");
  }

  // COMPANY user'larni id'lari bo'yicha qisqa ko'rinishda (cross-service).
  public Flux<CompanyBasicResponse> findAllByIds(List<UUID> companyIds) {
    return userRepository
        .findAllById(companyIds)
        .filter(u -> u.getType() == UserType.COMPANY)
        .map(u -> new CompanyBasicResponse(u.getId(), u.getLegalName(), null, u.getTin()));
  }

  // id COMPANY user'ga tegishlimi yoki oddiy foydalanuvchimi.
  public Mono<String> getUserTypeByCompanyId(UUID uuid) {
    log.debug("Determining type for ID: {}", uuid);
    return userRepository
        .findByIdAndDeletedIsFalse(uuid)
        .map(u -> u.getType() == UserType.COMPANY ? "COMPANY" : "CLIENT")
        .defaultIfEmpty("UNKNOWN");
  }

  // TIN bo'yicha COMPANY user + uning OWNER (staff) egasi.
  public Mono<CompanyWithOwner> getCompanyByInn(String inn) {
    return userRepository
        .findFirstByTinAndTypeAndDeletedFalseOrderByCreatedDateAsc(inn, UserType.COMPANY)
        .flatMap(
            company ->
                staffRepository
                    .findFirstByCompanyIdAndTypeAndStatusAndDeletedFalse(
                        company.getId(), StaffType.OWNER, StaffStatus.ACCEPTED)
                    .map(
                        owner ->
                            new CompanyWithOwner(
                                company.getId(),
                                company.getTin(),
                                company.getLegalName(),
                                owner.getUserId())));
  }
}
