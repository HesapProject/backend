package uz.hesap.service.main.api.v1;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.main.model.BannerRequest;
import uz.hesap.service.main.model.BannerResponse;
import uz.hesap.service.main.service.BannerService;

@Log4j2
@RestController
@RequiredArgsConstructor
@RequestMapping("/main/v1/banners")
public class BannerController {

  private final BannerService bannerService;

  // active=true → faqat is_active bannerlar (mobile/web, sort_order tartibida);
  // aks holda barchasi (admin).
  @GetMapping
  public Flux<BannerResponse> getAll(@RequestParam(required = false) Boolean active) {
    return Boolean.TRUE.equals(active) ? bannerService.findActive() : bannerService.findAll();
  }

  @GetMapping("/{id}")
  public Mono<BannerResponse> getById(@PathVariable UUID id) {
    return bannerService.findById(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Mono<BannerResponse> create(@RequestBody BannerRequest request) {
    return bannerService.create(request);
  }

  @PutMapping("/{id}")
  public Mono<BannerResponse> update(@PathVariable UUID id, @RequestBody BannerRequest request) {
    return bannerService.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public Mono<Void> delete(@PathVariable UUID id) {
    return bannerService.delete(id);
  }
}
