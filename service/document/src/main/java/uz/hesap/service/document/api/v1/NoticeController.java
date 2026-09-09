package uz.hesap.service.document.api.v1;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.UserPrincipal;
import uz.hesap.service.document.model.request.NoticeCreateRequest;
import uz.hesap.service.document.model.response.NoticeResponse;
import uz.hesap.service.document.service.c2c.NoticeService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/document/v1/notices")
public class NoticeController {

  private final NoticeService noticeService;

  // Menga kelgan talabnomalar (boshqa tomon yuborgan)
  // Talabnomalar: ?contractId bo'lsa — shu shartnoma bo'yicha; aks holda ?fromIn/?toIn filtri.
  @GetMapping
  public Mono<Page<NoticeResponse>> get(
      @RequestParam(value = "contractId", required = false) UUID contractId,
      @RequestParam(value = "fromIn", required = false) String fromIn,
      @RequestParam(value = "toIn", required = false) String toIn,
      @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
      @RequestParam(value = "size", required = false, defaultValue = "20") Integer size) {
    PageRequest pageable = PageRequest.of(page, size);
    if (contractId != null) {
      return noticeService.getAllByDocument(contractId, pageable);
    }
    return noticeService.getFiltered(fromIn, toIn, pageable);
  }

  @PostMapping
  public Mono<NoticeResponse> create(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @RequestBody NoticeCreateRequest request) {
    return noticeService.create(request, userPrincipal.user().id());
  }
}
