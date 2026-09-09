package uz.hesap.service.main.service;

import com.github.benmanes.caffeine.cache.Cache;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.hesap.service.main.domain.NewsEntity;
import uz.hesap.service.main.model.NewsRequest;
import uz.hesap.service.main.model.NewsResponse;
import uz.hesap.service.main.model.mapper.NewsMapper;
import uz.hesap.service.main.repository.NewsRepository;

@Log4j2
@Service
@RequiredArgsConstructor
public class NewsService {

  private final NewsRepository blogRepository;
  private final Cache<String, Page<NewsResponse>> newsCache;
  private static final String BLOG_CACHE = "blog_cache";

  public Mono<Page<NewsResponse>> getNews(Pageable pageable, String search, Boolean home) {
    if (Boolean.TRUE.equals(home)) {
      Page<NewsResponse> cached = newsCache.getIfPresent(BLOG_CACHE);
      if (cached != null) {
        return Mono.just(cached);
      }
      return getNewsFromDb(pageable, null, Boolean.TRUE)
          .doOnNext(db -> newsCache.put(BLOG_CACHE, db));
    }

    return getNewsFromDb(pageable, search, home);
  }

  public Mono<NewsResponse> getNews(UUID id) {
    return blogRepository.findByIdAndIsDeletedFalse(id).map(NewsMapper.INSTANCE::toResponse);
  }

  public Mono<NewsResponse> add(final NewsRequest request) {
    log.debug("Create blog {}", request);
    NewsEntity entity = NewsMapper.INSTANCE.toEntity(request);
    return blogRepository
        .save(entity)
        .map(NewsMapper.INSTANCE::toResponse)
        .doOnSuccess(b -> newsCache.invalidate(BLOG_CACHE));
  }

  public Mono<NewsResponse> edit(UUID id, NewsRequest request) {
    log.debug("Edit blog {} {} ", id, request);
    return blogRepository
        .findByIdAndIsDeletedFalse(id)
        .switchIfEmpty(Mono.error(new IllegalArgumentException("News not found: ")))
        .flatMap(
            existing -> {
              NewsMapper.INSTANCE.updateEntity(request, existing);
              return blogRepository.save(existing);
            })
        .doOnSuccess(b -> newsCache.invalidate(BLOG_CACHE))
        .map(NewsMapper.INSTANCE::toResponse);
  }

  public Mono<Void> delete(UUID id) {
    return blogRepository
        .findByIdAndIsDeletedFalse(id)
        .switchIfEmpty(Mono.error(new IllegalArgumentException("News not found: " + id)))
        .flatMap(
            entity -> {
              entity.setIsDeleted(Boolean.TRUE);
              return blogRepository.save(entity);
            })
        .doOnSuccess(b -> newsCache.invalidate(BLOG_CACHE))
        .then();
  }

  private Mono<Page<NewsResponse>> getNewsFromDb(Pageable pageable, String search, Boolean home) {
    return blogRepository
        .findNews(search, home, pageable)
        .map(
            page -> {
              List<NewsResponse> responses =
                  page.getContent().stream().map(NewsMapper.INSTANCE::toResponse).toList();
              return new PageImpl<>(responses, pageable, page.getTotalElements());
            });
  }
}
