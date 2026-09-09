package uz.hesap.service.main.api.v1;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.UserPrincipal;
import uz.hesap.service.main.model.request.ApiKeyRequest;
import uz.hesap.service.main.model.response.ApiKeyCreatedResponse;
import uz.hesap.service.main.model.response.ApiKeyResponse;
import uz.hesap.service.main.service.ApiKeyService;

/**
 * OpenAPI kalitlari. Kabinet (business) o'z kalitlarini shu yerdan boshqaradi; admin (Control)
 * token bilan kelganda `/list` orqali barcha kalitlarni ko'radi va istalgan mijoz nomidan
 * (request.ownerIn) kalit yarata oladi.
 */
@RestController
@RequestMapping("/main/v1/api-keys")
@RequiredArgsConstructor
public class ApiKeyController {

  private final ApiKeyService apiKeyService;

  // Joriy foydalanuvchi/kompaniyaning kalitlari.
  @GetMapping
  public Flux<ApiKeyResponse> getMine(@AuthenticationPrincipal UserPrincipal userPrincipal) {
    return apiKeyService.getMine(userPrincipal);
  }

  // Barcha kalitlar (Control) — search: owner_in yoki kalit nomi bo'yicha.
  @GetMapping("/list")
  public Mono<Page<ApiKeyResponse>> getAll(
      @RequestParam(required = false) String search,
      @RequestParam(required = false, defaultValue = "0") Integer page,
      @RequestParam(required = false, defaultValue = "20") Integer size) {
    Pageable pageable = PageRequest.of(page, size);
    return apiKeyService.getAllAdmin(search, pageable);
  }

  // Kalit yaratish — javobdagi `key` faqat SHU YERDA ko'rinadi.
  @PostMapping
  public Mono<ApiKeyCreatedResponse> create(
      @AuthenticationPrincipal UserPrincipal userPrincipal, @RequestBody ApiKeyRequest request) {
    return apiKeyService.create(userPrincipal, request);
  }

  @PutMapping("/{id}")
  public Mono<ApiKeyResponse> update(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @PathVariable UUID id,
      @RequestBody ApiKeyRequest request) {
    return apiKeyService.update(userPrincipal, id, request);
  }

  // Yangi kalit matni — eskisi shu zahoti ishlamay qoladi.
  @PostMapping("/{id}/rotate")
  public Mono<ApiKeyCreatedResponse> rotate(
      @AuthenticationPrincipal UserPrincipal userPrincipal, @PathVariable UUID id) {
    return apiKeyService.rotate(userPrincipal, id);
  }

  @PostMapping("/{id}/revoke")
  public Mono<ApiKeyResponse> revoke(
      @AuthenticationPrincipal UserPrincipal userPrincipal, @PathVariable UUID id) {
    return apiKeyService.revoke(userPrincipal, id);
  }

  @DeleteMapping("/{id}")
  public Mono<Void> delete(
      @AuthenticationPrincipal UserPrincipal userPrincipal, @PathVariable UUID id) {
    return apiKeyService.delete(userPrincipal, id);
  }
}
