package uz.hesap.service.main.api.v1;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.enums.TariffType;
import uz.hesap.service.main.model.PackageRequest;
import uz.hesap.service.main.model.PackageResponse;
import uz.hesap.service.main.service.PackageService;

@RestController
@RequestMapping("/main/v1/packages")
@RequiredArgsConstructor
@Log4j2
public class PackageController {

  private final PackageService packageService;

  // Hammasini qaytaradi.
  @GetMapping
  public Flux<PackageResponse> getAll(@RequestParam(required = false) TariffType type) {
    return packageService.getAll(type);
  }

  // Yangi yaratadi.
  @PostMapping
  public Mono<PackageResponse> create(@RequestBody PackageRequest request) {
    return packageService.create(request);
  }

  // Edit qiladi.
  @PutMapping("/{id}")
  public Mono<PackageResponse> update(@PathVariable UUID id, @RequestBody PackageRequest request) {
    return packageService.update(id, request);
  }

  // Delete qiladi.
  @DeleteMapping("/{id}")
  public Mono<Void> delete(@PathVariable UUID id) {
    return packageService.delete(id);
  }
}
