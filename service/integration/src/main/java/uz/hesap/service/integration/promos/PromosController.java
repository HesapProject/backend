package uz.hesap.service.integration.promos;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/integration/v1/promos")
@RequiredArgsConstructor
@Log4j2
public class PromosController {

  private final PromosService promosService;

  @GetMapping("/all")
  public Flux<PromosResponse> getAll() {
    return promosService.getAll();
  }

  @GetMapping("/{id}")
  public Mono<PromosResponse> getById(@PathVariable UUID id) {
    return promosService.getById(id);
  }

  @GetMapping("/code/{code}")
  public Mono<PromosResponse> findByCode(@PathVariable String code) {
    return promosService.findByCode(code);
  }

  // Promokodni check qilish — xato tashlamaydi: {valid, message, promo} qaytaradi.
  @GetMapping("/check/{code}")
  public Mono<PromosCheckResponse> check(@PathVariable String code) {
    return promosService.check(code);
  }

  @PostMapping
  public Mono<PromosResponse> create(@RequestBody PromosRequest request) {
    return promosService.create(request);
  }

  @PutMapping("/{id}")
  public Mono<PromosResponse> update(@PathVariable UUID id, @RequestBody PromosRequest request) {
    return promosService.update(id, request);
  }

  @DeleteMapping("/{id}")
  public Mono<Void> delete(@PathVariable UUID id) {
    return promosService.delete(id);
  }

  @GetMapping()
  public Mono<Page<PromosResponse>> getAllPaged(Pageable pageable) {
    return promosService.getAllPaged(pageable);
  }
}
