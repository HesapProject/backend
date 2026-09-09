package uz.hesap.service.document.api.openapi;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.exception.NotFoundException;
import uz.hesap.service.common.util.ApiKeyContext;
import uz.hesap.service.common.util.UserPrincipal;
import uz.hesap.service.common.util.enums.ApiScope;
import uz.hesap.service.document.webclient.UserServiceClient;

/**
 * Public API — kalitning o'zi haqidagi ma'lumot va taraf (mijoz) tekshiruvi. Integratsiyani
 * sozlashda "kalit ishlayaptimi, qaysi huquqlari bor?" savoliga javob beradi.
 */
@Tag(name = "Umumiy", description = "Kalit holati va taraf tekshiruvi")
@RestController
@RequestMapping("/openapi/v1")
@RequiredArgsConstructor
public class OpenApiMetaController {

  private final UserServiceClient userServiceClient;

  @Operation(summary = "Kalit ma'lumoti — huquqlar, shablon cheklovi, muddat")
  @GetMapping("/me")
  public Mono<ApiKeyInfoResponse> me(@AuthenticationPrincipal UserPrincipal userPrincipal) {
    ApiKeyContext apiKey = OpenApiGuard.context(userPrincipal);
    return Mono.just(
        new ApiKeyInfoResponse(
            apiKey.name(),
            apiKey.ownerIn(),
            userPrincipal.user() != null ? userPrincipal.user().legalName() : null,
            apiKey.scopes(),
            apiKey.allTemplates(),
            apiKey.templateIds(),
            apiKey.expiresAt()));
  }

  @Operation(summary = "Taraf (mijoz) ma'lumoti PINFL/STIR bo'yicha")
  @GetMapping("/clients/{in}")
  public Mono<ClientResponse> client(
      @AuthenticationPrincipal UserPrincipal userPrincipal, @PathVariable String in) {
    OpenApiGuard.requireScope(userPrincipal, ApiScope.CLIENTS_READ);
    return userServiceClient
        .getUserByIn(in)
        .switchIfEmpty(Mono.error(new NotFoundException("Bunday PINFL/STIR topilmadi")))
        .map(
            user ->
                new ClientResponse(
                    user.identifier(),
                    user.firstName(),
                    user.lastName(),
                    user.midName(),
                    user.legalName(),
                    user.type() != null ? user.type().name() : null,
                    user.verified()));
  }

  public record ApiKeyInfoResponse(
      String name,
      String ownerIn,
      String ownerLegalName,
      List<ApiScope> scopes,
      Boolean allTemplates,
      List<UUID> templateIds,
      Instant expiresAt) {}

  // Public API'da faqat identifikatsiya uchun zarur maydonlar (passport/telefon yo'q).
  public record ClientResponse(
      String in,
      String firstName,
      String lastName,
      String midName,
      String legalName,
      String type,
      Boolean verified) {}
}
