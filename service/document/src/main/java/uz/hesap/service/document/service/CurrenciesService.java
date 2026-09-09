package uz.hesap.service.document.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.exception.AlreadyExistsException;
import uz.hesap.service.common.exception.BadRequestException;
import uz.hesap.service.common.exception.NotFoundException;
import uz.hesap.service.document.domain.CurrencyEntity;
import uz.hesap.service.document.model.mapper.CurrencyMapper;
import uz.hesap.service.document.model.request.CurrencyRequest;
import uz.hesap.service.document.model.response.CurrencyResponse;
import uz.hesap.service.document.repository.CurrencyRepository;

@Service
@RequiredArgsConstructor
public class CurrenciesService {

  private final CurrencyRepository repository;

  /** Admin: barcha valyutalar (deleted bo'lmaganlar). */
  public Flux<CurrencyResponse> getAll() {
    return repository
        .findAllByDeletedFalseOrderBySortOrderAsc()
        .map(CurrencyMapper.INSTANCE::toResponse);
  }

  /** Public: faqat active valyutalar. */
  public Flux<CurrencyResponse> getAllActive() {
    return repository
        .findAllByDeletedFalseAndIsActiveTrueOrderBySortOrderAsc()
        .map(CurrencyMapper.INSTANCE::toResponse);
  }

  public Mono<CurrencyResponse> getById(UUID id) {
    return repository
        .findByIdAndDeletedFalse(id)
        .switchIfEmpty(Mono.error(new NotFoundException("Valyuta topilmadi: " + id)))
        .map(CurrencyMapper.INSTANCE::toResponse);
  }

  public Mono<CurrencyResponse> create(CurrencyRequest request) {
    if (request.code() == null || request.code().isBlank()) {
      return Mono.error(new BadRequestException("Valyuta kodi bo'sh bo'lmasligi kerak"));
    }
    String code = request.code().trim().toUpperCase();
    return repository
        .findByCodeAndDeletedFalse(code)
        .flatMap(
            existing ->
                Mono.<CurrencyEntity>error(
                    new AlreadyExistsException("Bunday kod allaqachon mavjud: " + code)))
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  CurrencyEntity entity = CurrencyMapper.INSTANCE.toEntity(request);
                  entity.setCode(code);
                  if (entity.getIsActive() == null) entity.setIsActive(Boolean.TRUE);
                  if (entity.getSortOrder() == null) entity.setSortOrder(0);
                  return repository.save(entity);
                }))
        .map(CurrencyMapper.INSTANCE::toResponse);
  }

  public Mono<CurrencyResponse> update(UUID id, CurrencyRequest request) {
    return repository
        .findByIdAndDeletedFalse(id)
        .switchIfEmpty(Mono.error(new NotFoundException("Valyuta topilmadi: " + id)))
        .flatMap(
            entity -> {
              CurrencyMapper.INSTANCE.updateEntity(request, entity);
              if (request.code() != null && !request.code().isBlank()) {
                entity.setCode(request.code().trim().toUpperCase());
              }
              return repository.save(entity);
            })
        .map(CurrencyMapper.INSTANCE::toResponse);
  }

  public Mono<Void> delete(UUID id) {
    return repository
        .findByIdAndDeletedFalse(id)
        .switchIfEmpty(Mono.error(new NotFoundException("Valyuta topilmadi: " + id)))
        .flatMap(
            entity -> {
              entity.setDeleted(Boolean.TRUE);
              return repository.save(entity);
            })
        .then();
  }
}
