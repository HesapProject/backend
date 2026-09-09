package uz.hesap.service.document.service.c2c;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.document.model.response.DocumentEnrichedResponse;
import uz.hesap.service.document.model.response.PartnerDocCount;
import uz.hesap.service.document.model.response.PartnerResponse;
import uz.hesap.service.document.repository.PartnerRepository;
import uz.hesap.service.document.service.document.ContractsService;
import uz.hesap.service.document.webclient.UserServiceClient;

@Service
@RequiredArgsConstructor
@Log4j2
public class PartnerService {

  private final PartnerRepository partnerRepository;
  private final UserServiceClient userServiceClient;
  private final ContractsService documentService;

  // userni barcha partnerlari + doc count
  public Flux<PartnerResponse> getPartners(UUID userId) {
    return partnerRepository
        .findPartnerCounts(userId)
        .collectList()
        .flatMapMany(this::enrichWithUserInfo);
  }

  // IN (PINFL/STIR) bo'yicha hamkorlar — profil "Hamkorlar" tabi uchun (id emas, in).
  public Flux<PartnerResponse> getPartnersByIn(String in) {
    return partnerRepository
        .findPartnerCountsByIn(in)
        .collectList()
        .flatMapMany(this::enrichWithUserInfo);
  }

  // userId va partnerId orasidagi o'zaro shartnomalar (har ikkala id → in resolve qilinadi).
  public Mono<Page<DocumentEnrichedResponse>> getContractsBetween(
      UUID userId, UUID partnerId, Pageable pageable) {
    return Mono.zip(partnerRepository.findInById(userId), partnerRepository.findInById(partnerId))
        .flatMap(t -> documentService.getBetween(t.getT1(), t.getT2(), pageable));
  }

  // partner id lar bo'yicha user servicega chiqib ma'lumot olish
  private Flux<PartnerResponse> enrichWithUserInfo(List<PartnerDocCount> partners) {
    if (partners.isEmpty()) return Flux.empty();

    List<UUID> partnerIds = partners.stream().map(PartnerDocCount::partnerId).toList();

    Map<UUID, Long> countMap =
        partners.stream()
            .collect(Collectors.toMap(PartnerDocCount::partnerId, PartnerDocCount::docCount));

    return userServiceClient
        .getUsersByIds(partnerIds)
        .map(user -> new PartnerResponse(user, countMap.getOrDefault(user.id(), 0L)));
  }
}
