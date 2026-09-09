package uz.hesap.service.main.service;

import com.github.benmanes.caffeine.cache.Cache;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.hesap.service.main.domain.FaqEntity;
import uz.hesap.service.main.model.FaqRequest;
import uz.hesap.service.main.model.FaqResponse;
import uz.hesap.service.main.model.mapper.FaqMapper;
import uz.hesap.service.main.repository.FaqRepository;

@Log4j2
@Service
@RequiredArgsConstructor
public class FaqService {

  private final FaqRepository faqRepository;
  private final Cache<String, List<FaqResponse>> faqCache;

  private static final String FAQ_CACHE_KEY = "faq_list_cache";

  public Mono<List<FaqResponse>> getAllFaqs() {
    log.debug("Fetching all FAQs");

    // Check cache first
    List<FaqResponse> cached = faqCache.getIfPresent(FAQ_CACHE_KEY);
    if (cached != null) {
      log.debug("Returning cached FAQ list, size: {}", cached.size());
      return Mono.just(cached);
    }

    // Fetch from database and cache
    return faqRepository
        .findAllByIsDeletedFalseOrderByCreatedDateDesc()
        .map(FaqMapper.INSTANCE::toResponse)
        .collectList()
        .doOnNext(
            list -> {
              log.debug("Caching FAQ list, size: {}", list.size());
              faqCache.put(FAQ_CACHE_KEY, list);
            });
  }

  public Mono<FaqResponse> getFaqById(UUID id) {
    log.debug("Fetching FAQ by id: {}", id);
    return faqRepository.findByIdAndIsDeletedFalse(id).map(FaqMapper.INSTANCE::toResponse);
  }

  public Mono<FaqResponse> createFaq(FaqRequest request) {
    log.info("Creating new FAQ with title: {}", request);

    FaqEntity entity = FaqMapper.INSTANCE.toEntity(request);

    return faqRepository
        .save(entity)
        .doOnSuccess(
            saved -> {
              log.info("FAQ created successfully with id: {}", saved.getId());
              invalidateCache();
            })
        .map(FaqMapper.INSTANCE::toResponse);
  }

  public Mono<FaqResponse> updateFaq(UUID id, FaqRequest request) {
    log.info("Updating FAQ with id: {}", id);

    return faqRepository
        .findByIdAndIsDeletedFalse(id)
        .switchIfEmpty(Mono.error(new IllegalArgumentException("FAQ not found with id: " + id)))
        .flatMap(
            existingEntity -> {
              FaqMapper.INSTANCE.updateEntity(request, existingEntity);
              return faqRepository.save(existingEntity);
            })
        .doOnSuccess(updated -> invalidateCache())
        .map(FaqMapper.INSTANCE::toResponse);
  }

  public Mono<Void> deleteFaq(UUID id) {
    log.info("Deleting FAQ with id: {}", id);

    return faqRepository
        .findByIdAndIsDeletedFalse(id)
        .switchIfEmpty(Mono.error(new IllegalArgumentException("FAQ not found with id: " + id)))
        .flatMap(
            entity -> {
              entity.setIsDeleted(Boolean.TRUE);
              return faqRepository.save(entity);
            })
        .doOnSuccess(
            deleted -> {
              log.info("FAQ deleted successfully with id: {}", id);
              invalidateCache();
            })
        .then();
  }

  private void invalidateCache() {
    log.debug("Invalidating FAQ cache");
    faqCache.invalidate(FAQ_CACHE_KEY);
  }
}
