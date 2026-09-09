package uz.hesap.service.integration.promos;

import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.exception.AlreadyExistsException;
import uz.hesap.service.common.exception.NotFoundException;

@Service
@RequiredArgsConstructor
@Log4j2
public class PromosService {

  private final PromosRepository promosRepository;
  private final PromosMapper promosMapper;

  public Mono<PromosResponse> create(final PromosRequest request) {
    log.debug("Creating new promo with request: {}", request);

    return promosRepository
        .findByCodeAndDeletedFalse(request.code())
        .flatMap(
            pc ->
                Mono.<PromosResponse>error(new AlreadyExistsException("Promo code already exists")))
        .switchIfEmpty(
            Mono.defer(
                () ->
                    promosRepository
                        .save(promosMapper.toEntity(request))
                        .map(promosMapper::toResponse)));
  }

  public Mono<PromosResponse> update(final UUID id, final PromosRequest request) {
    log.debug("Updating promo with id: {} and request: {}", id, request);

    return promosRepository
        .findByIdAndDeletedFalse(id)
        .switchIfEmpty(
            Mono.error(
                () -> {
                  log.error("Promo not found with id: {} for update", id);
                  return new NotFoundException("Promo not found with id: " + id);
                }))
        .flatMap(
            existingEntity -> {
              boolean codeHasChanged = !existingEntity.getCode().equals(request.code());
              if (codeHasChanged) {
                return promosRepository
                    .findByCodeAndDeletedFalse(request.code())
                    .flatMap(
                        conflictingEntity ->
                            Mono.<PromosEntity>error(
                                new AlreadyExistsException(
                                    "Promo code '" + request.code() + "' already exists.")))
                    .switchIfEmpty(
                        Mono.defer(
                            () -> {
                              promosMapper.toUpdate(request, existingEntity);
                              return promosRepository.save(existingEntity);
                            }));
              } else {
                promosMapper.toUpdate(request, existingEntity);
                return promosRepository.save(existingEntity);
              }
            })
        .map(promosMapper::toResponse);
  }

  public Flux<PromosResponse> getAll() {
    log.debug("Fetching all promos");
    return promosRepository.findAllByDeletedFalse().map(promosMapper::toResponse);
  }

  public Mono<PromosResponse> getById(final UUID id) {
    log.debug("Fetching promo with id: {}", id);
    return promosRepository
        .findByIdAndDeletedFalse(id)
        .map(promosMapper::toResponse)
        .switchIfEmpty(
            Mono.error(
                () -> {
                  log.warn("Promo not found");
                  return new NotFoundException("Promo not found ");
                }));
  }

  public Mono<PromosResponse> findByCode(final String code) {
    log.debug("Fetching promo with code: {}", code);
    return promosRepository
        .findByCodeAndDeletedFalse(code)
        .map(promosMapper::toResponse)
        .switchIfEmpty(
            Mono.error(
                () -> {
                  log.warn("Promo not found ");
                  return new NotFoundException("Promo not found");
                }));
  }

  // Promokodni tekshirish — xato tashlamaydi: natija {valid, message, promo}.
  // Client checkout'da promokodni qo'llashdan oldin holatini bilish uchun.
  public Mono<PromosCheckResponse> check(final String code) {
    log.debug("Checking promo: {}", code);
    final LocalDate today = LocalDate.now();
    return promosRepository
        .findByCodeAndDeletedFalse(code)
        .map(
            p -> {
              if (Boolean.FALSE.equals(p.getActive())) {
                return new PromosCheckResponse(false, "Promokod faol emas", null);
              }
              if (p.getValidFrom() != null && today.isBefore(p.getValidFrom())) {
                return new PromosCheckResponse(false, "Promokod hali boshlanmagan", null);
              }
              if (p.getValidTo() != null && today.isAfter(p.getValidTo())) {
                return new PromosCheckResponse(false, "Promokod muddati o'tgan", null);
              }
              return new PromosCheckResponse(true, "OK", promosMapper.toResponse(p));
            })
        .defaultIfEmpty(new PromosCheckResponse(false, "Promokod topilmadi", null));
  }

  public Mono<Void> delete(final UUID id) {
    log.debug("Deleting promo with id: {}", id);
    return promosRepository
        .findByIdAndDeletedFalse(id)
        .switchIfEmpty(
            Mono.error(
                () -> {
                  log.warn("Promo not found with id: {} for deletion", id);
                  return new NotFoundException("Promo not found with id: " + id);
                }))
        .flatMap(
            promo -> {
              promo.setDeleted(true);
              return promosRepository.save(promo);
            })
        .then();
  }

  public Mono<Page<PromosResponse>> getAllPaged(Pageable pageable) {
    log.debug("Fetching paged promos with page: {}", pageable);
    Flux<PromosResponse> promosResponseFlux =
        promosRepository.findAllByDeletedFalse(pageable).map(promosMapper::toResponse);

    Mono<Long> totalCountMono = promosRepository.countByDeletedFalse();

    return Mono.zip(promosResponseFlux.collectList(), totalCountMono)
        .map(tuple -> new PageImpl<>(tuple.getT1(), pageable, tuple.getT2()));
  }
}
