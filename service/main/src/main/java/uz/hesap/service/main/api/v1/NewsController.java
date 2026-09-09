package uz.hesap.service.main.api.v1;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import uz.hesap.service.main.model.NewsRequest;
import uz.hesap.service.main.model.NewsResponse;
import uz.hesap.service.main.model.SendNewsNotification;
import uz.hesap.service.main.service.NewsNotificationService;
import uz.hesap.service.main.service.NewsService;

@RestController
@RequiredArgsConstructor
// Yangi yo'l /main/v1/news; /main/v1/blogs — eski mobil ilovalar uchun legacy alias.
@RequestMapping({"/main/v1/news", "/main/v1/blogs"})
public class NewsController {

  private final NewsService newsService;
  private final NewsNotificationService newsNotificationService;

  @PostMapping
  public Mono<NewsResponse> createNews(@RequestBody NewsRequest request) {
    return newsService.add(request);
  }

  @PostMapping("/publish")
  public Mono<Void> publish(@RequestBody SendNewsNotification request) {
    return newsNotificationService.publishNews(request);
  }

  @PutMapping("/{id}")
  public Mono<NewsResponse> updateNews(@PathVariable UUID id, @RequestBody NewsRequest request) {
    return newsService
        .edit(id, request)
        .onErrorMap(
            IllegalArgumentException.class,
            e -> new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage()));
  }

  @DeleteMapping("/{id}")
  public Mono<Void> deleteNews(@PathVariable UUID id) {
    return newsService
        .delete(id)
        .onErrorMap(
            IllegalArgumentException.class,
            e -> new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage()));
  }

  @GetMapping
  public Mono<Page<NewsResponse>> getNews(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) Boolean home,
      Pageable pageable) {
    return newsService.getNews(pageable, search, home);
  }

  @GetMapping("/{id}")
  public Mono<NewsResponse> getNews(@PathVariable UUID id) {
    return newsService
        .getNews(id)
        .switchIfEmpty(
            Mono.error(new ResponseStatusException(HttpStatus.NO_CONTENT, "News not found")));
  }
}
