package uz.hesap.service.integration.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.hesap.service.integration.domain.OneIdUserEntity;
import uz.hesap.service.integration.model.mapper.OneIdUserMapper;
import uz.hesap.service.integration.model.oneid.OneIdPassportResponse;
import uz.hesap.service.integration.model.oneid.OneIdUserResponse;
import uz.hesap.service.integration.repository.OneIdUserRepository;

// OneID profil ma'lumotlarini saqlash va o'qish.
// main-service'ning OneIdService shu service'ni Feign orqali ishlatadi.
@Service
@RequiredArgsConstructor
@Log4j2
public class OneIdProfileService {

  private final OneIdUserRepository repository;
  private final OneIdUserMapper mapper = OneIdUserMapper.INSTANCE;

  // userId — primary key (one_id_user.id == user.users.id). UPSERT semantikasi.
  public Mono<OneIdUserEntity> save(UUID userId, OneIdUserResponse response) {
    return repository
        .existsById(userId)
        .map(
            exists -> {
              OneIdUserEntity entity = mapper.toEntity(userId, response);
              if (Boolean.TRUE.equals(exists)) {
                entity.markNotNew();
              }
              return entity;
            })
        .flatMap(repository::save);
  }

  public Mono<OneIdPassportResponse> getPassport(UUID userId) {
    return repository.findById(userId).map(mapper::toPassportResponse);
  }
}
