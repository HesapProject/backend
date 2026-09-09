package uz.hesap.service.main.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.exception.NotFoundException;
import uz.hesap.service.common.util.TextModel;
import uz.hesap.service.main.domain.BannerEntity;
import uz.hesap.service.main.model.BannerRequest;
import uz.hesap.service.main.model.BannerResponse;
import uz.hesap.service.main.repository.BannerRepository;

@Service
@RequiredArgsConstructor
@Log4j2
public class BannerService {

  private final BannerRepository repository;

  public Flux<BannerResponse> findAll() {
    return repository.findAllByIsDeletedFalseOrderBySortOrderAsc().map(this::toResponse);
  }

  public Flux<BannerResponse> findActive() {
    return repository
        .findAllByIsDeletedFalseAndIsActiveTrueOrderBySortOrderAsc()
        .map(this::toResponse);
  }

  public Mono<BannerResponse> findById(UUID id) {
    return repository
        .findById(id)
        .filter(b -> !Boolean.TRUE.equals(b.getIsDeleted()))
        .switchIfEmpty(Mono.error(new NotFoundException("Banner topilmadi: " + id)))
        .map(this::toResponse);
  }

  public Mono<BannerResponse> create(BannerRequest request) {
    BannerEntity entity = new BannerEntity();
    apply(entity, request);
    return repository.save(entity).map(this::toResponse);
  }

  public Mono<BannerResponse> update(UUID id, BannerRequest request) {
    return repository
        .findById(id)
        .filter(b -> !Boolean.TRUE.equals(b.getIsDeleted()))
        .switchIfEmpty(Mono.error(new NotFoundException("Banner topilmadi: " + id)))
        .flatMap(
            entity -> {
              apply(entity, request);
              return repository.save(entity);
            })
        .map(this::toResponse);
  }

  public Mono<Void> delete(UUID id) {
    return repository
        .findById(id)
        .switchIfEmpty(Mono.error(new NotFoundException("Banner topilmadi: " + id)))
        .flatMap(
            b -> {
              b.setIsDeleted(Boolean.TRUE);
              return repository.save(b);
            })
        .then();
  }

  private void apply(BannerEntity entity, BannerRequest request) {
    if (request.title() != null) {
      entity.setTitleUz(request.title().uz());
      entity.setTitleRu(request.title().ru());
      entity.setTitleEn(request.title().en());
    }
    if (request.image() != null) entity.setImage(request.image());
    if (request.link() != null) entity.setLink(request.link());
    if (request.sortOrder() != null) entity.setSortOrder(request.sortOrder());
    if (request.isActive() != null) entity.setIsActive(request.isActive());
  }

  private BannerResponse toResponse(BannerEntity e) {
    return new BannerResponse(
        e.getId(),
        new TextModel(e.getTitleUz(), e.getTitleRu(), e.getTitleEn()),
        e.getImage(),
        e.getLink(),
        e.getSortOrder(),
        e.getIsActive(),
        e.getCreatedDate());
  }
}
