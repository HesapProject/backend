package uz.hesap.service.main.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.UserPrincipal;
import uz.hesap.service.main.domain.SocialEntity;
import uz.hesap.service.main.model.request.SocialRequest;
import uz.hesap.service.main.model.response.SocialResponse;
import uz.hesap.service.main.repository.SocialRepository;

@Log4j2
@Service
@RequiredArgsConstructor
public class SocialService {

  private final SocialRepository socialRepository;

  public Mono<SocialResponse> add(UserPrincipal userPrincipal, SocialRequest request) {
    String userIn = userPrincipal.user().identifier();
    return socialRepository
        .findByUserIn(userIn)
        .flatMap(
            existing -> {
              existing.setPhone2(request.phone2());
              existing.setTelegram(request.telegram());
              existing.setInstagram(request.instagram());
              existing.setFacebook(request.facebook());
              return socialRepository.save(existing);
            })
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  SocialEntity newSocial = new SocialEntity();
                  newSocial.setUserIn(userIn);
                  newSocial.setPhone2(request.phone2());
                  newSocial.setTelegram(request.telegram());
                  newSocial.setInstagram(request.instagram());
                  newSocial.setFacebook(request.facebook());
                  return socialRepository.save(newSocial);
                }))
        .map(this::toResponse);
  }

  public Mono<SocialResponse> get(UserPrincipal userPrincipal) {
    return socialRepository.findByUserIn(userPrincipal.user().identifier()).map(this::toResponse);
  }

  private SocialResponse toResponse(SocialEntity entity) {
    return new SocialResponse(
        entity.getId(),
        entity.getUserIn(),
        entity.getPhone2(),
        entity.getTelegram(),
        entity.getInstagram(),
        entity.getFacebook());
  }
}
