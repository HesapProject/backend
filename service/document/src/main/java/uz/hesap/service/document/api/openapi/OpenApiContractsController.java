package uz.hesap.service.document.api.openapi;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.exception.BadRequestException;
import uz.hesap.service.common.exception.ForbiddenException;
import uz.hesap.service.common.util.ApiKeyContext;
import uz.hesap.service.common.util.UserPrincipal;
import uz.hesap.service.common.util.enums.ApiScope;
import uz.hesap.service.document.api.openapi.model.OpenApiContractRequest;
import uz.hesap.service.document.domain.enums.DocumentStatus;
import uz.hesap.service.document.model.request.DocumentRequest;
import uz.hesap.service.document.model.response.CreatedDocumentResponse;
import uz.hesap.service.document.model.response.DocumentEnrichedResponse;
import uz.hesap.service.document.service.document.ContractsService;
import uz.hesap.service.document.service.document.DocumentGenerateService;

/**
 * Public API — shartnomalar. Barcha amallar kalit EGASI nomidan bajariladi: yaratilgan
 * shartnomaning yaratuvchisi (creatorIn) kalit egasi bo'ladi, ro'yxat esa faqat u ishtirok
 * etgan shartnomalarni qaytaradi.
 */
@Tag(name = "Shartnomalar", description = "API orqali shartnoma tuzish va o'qish")
@RestController
@RequestMapping("/openapi/v1/contracts")
@RequiredArgsConstructor
public class OpenApiContractsController {

  private final ContractsService contractsService;
  private final DocumentGenerateService documentGenerateService;

  @Operation(summary = "Shartnoma yaratish")
  @PostMapping
  public Mono<CreatedDocumentResponse> create(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @RequestBody OpenApiContractRequest request) {
    ApiKeyContext apiKey = OpenApiGuard.requireScope(userPrincipal, ApiScope.CONTRACTS_WRITE);
    OpenApiGuard.requireTemplate(apiKey, request.templateId());

    String ownerIn = OpenApiGuard.ownerIn(userPrincipal);
    DocumentRequest documentRequest = withOwnerParty(request, ownerIn);

    return contractsService
        .create(documentRequest, userPrincipal.user().id(), ownerIn)
        .map(CreatedDocumentResponse::new);
  }

  @Operation(summary = "Shartnomalar ro'yxati (kalit egasi ishtirok etgan)")
  @GetMapping
  public Mono<Page<DocumentEnrichedResponse>> getAll(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @RequestParam(required = false) List<DocumentStatus> statuses,
      @RequestParam(required = false) UUID templateId,
      @RequestParam(required = false) String search,
      @RequestParam(required = false, defaultValue = "0") Integer page,
      @RequestParam(required = false, defaultValue = "20") Integer size) {
    ApiKeyContext apiKey = OpenApiGuard.requireScope(userPrincipal, ApiScope.CONTRACTS_READ);
    if (templateId != null && !apiKey.allowsTemplate(templateId)) {
      return Mono.error(new ForbiddenException("Bu shablon kalitga ruxsat etilmagan"));
    }
    Pageable pageable = PageRequest.of(page, Math.min(size, 100));
    return contractsService
        .getAll(OpenApiGuard.ownerIn(userPrincipal), pageable, statuses, templateId, false, search, null, null)
        // Full bo'lmagan kalit faqat o'ziga ruxsat etilgan shablon hujjatlarini ko'radi.
        .map(result -> filterByTemplateScope(result, apiKey, pageable));
  }

  @Operation(summary = "Bitta shartnoma")
  @GetMapping("/{id}")
  public Mono<DocumentEnrichedResponse> getById(
      @AuthenticationPrincipal UserPrincipal userPrincipal, @PathVariable UUID id) {
    ApiKeyContext apiKey = OpenApiGuard.requireScope(userPrincipal, ApiScope.CONTRACTS_READ);
    String ownerIn = OpenApiGuard.ownerIn(userPrincipal);
    return contractsService.getById(id).flatMap(doc -> authorize(doc, apiKey, ownerIn));
  }

  @Operation(summary = "Shartnoma PDF (application/pdf)")
  @GetMapping("/{id}/pdf")
  public Mono<Void> pdf(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @PathVariable UUID id,
      @RequestParam(required = false) String lang,
      ServerHttpResponse response) {
    ApiKeyContext apiKey = OpenApiGuard.requireScope(userPrincipal, ApiScope.CONTRACTS_READ);
    String ownerIn = OpenApiGuard.ownerIn(userPrincipal);
    return contractsService
        .getById(id)
        .flatMap(doc -> authorize(doc, apiKey, ownerIn))
        .flatMap(doc -> documentGenerateService.generate(id, lang))
        .flatMap(pdf -> writePdf(response, pdf));
  }

  // ======================== HELPERS ========================

  // Taraflardan biri kalit egasi bo'lishi shart. Bo'sh tomon egasi bilan to'ldiriladi;
  // ikkalasi ham begona bo'lsa — uchinchi shaxslar nomidan shartnoma tuzishga yo'l qo'ymaymiz.
  private DocumentRequest withOwnerParty(final OpenApiContractRequest request, final String ownerIn) {
    boolean buyerBlank = isBlank(request.buyerIn());
    boolean sellerBlank = isBlank(request.sellerIn());
    if (buyerBlank && sellerBlank) {
      throw new BadRequestException("buyerIn yoki sellerIn ko'rsatilishi kerak");
    }
    if (!buyerBlank
        && !sellerBlank
        && !ownerIn.equals(request.buyerIn())
        && !ownerIn.equals(request.sellerIn())) {
      throw new ForbiddenException("Shartnoma taraflaridan biri kalit egasi bo'lishi kerak");
    }
    return request.toDocumentRequest();
  }

  // Hujjat kalit egasiga tegishli va shabloni kalitga ruxsat etilganmi.
  private Mono<DocumentEnrichedResponse> authorize(
      final DocumentEnrichedResponse doc, final ApiKeyContext apiKey, final String ownerIn) {
    if (!OpenApiGuard.belongsTo(ownerIn, doc.buyerIn(), doc.sellerIn(), doc.creatorIn())) {
      return Mono.error(new ForbiddenException("Bu shartnoma kalit egasiga tegishli emas"));
    }
    if (!apiKey.allowsTemplate(doc.templateId())) {
      return Mono.error(new ForbiddenException("Bu shablon kalitga ruxsat etilmagan"));
    }
    return Mono.just(doc);
  }

  // Sahifa ichidan ruxsat etilmagan shablon hujjatlarini olib tashlaydi. totalElements
  // filtrlashdan oldingi qiymatda qoladi — bu faqat full bo'lmagan kalitga taalluqli.
  private Page<DocumentEnrichedResponse> filterByTemplateScope(
      final Page<DocumentEnrichedResponse> source,
      final ApiKeyContext apiKey,
      final Pageable pageable) {
    if (Boolean.TRUE.equals(apiKey.allTemplates())) {
      return source;
    }
    List<DocumentEnrichedResponse> allowed =
        source.getContent().stream().filter(d -> apiKey.allowsTemplate(d.templateId())).toList();
    return new org.springframework.data.domain.PageImpl<>(
        allowed, pageable, source.getTotalElements());
  }

  private static boolean isBlank(final String value) {
    return value == null || value.isBlank();
  }

  // Raw baytlarni yozamiz — WebFlux content negotiation (Accept) chetlab o'tiladi.
  private static Mono<Void> writePdf(final ServerHttpResponse response, final byte[] pdf) {
    response.setStatusCode(HttpStatus.OK);
    HttpHeaders headers = response.getHeaders();
    headers.setContentType(MediaType.APPLICATION_PDF);
    headers.setContentLength(pdf.length);
    headers.setContentDisposition(ContentDisposition.inline().filename("contract.pdf").build());
    DataBuffer buffer = response.bufferFactory().wrap(pdf);
    return response.writeWith(Mono.just(buffer));
  }
}
