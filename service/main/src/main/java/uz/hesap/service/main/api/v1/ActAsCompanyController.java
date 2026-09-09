package uz.hesap.service.main.api.v1;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.UserPrincipal;
import uz.hesap.service.main.model.request.CompanyTokenRequest;
import uz.hesap.service.main.model.response.JwtTokenResponse;
import uz.hesap.service.main.service.ActAsCompanyService;

// Alohida endpoint: person token bilan companyId yuborilib, shu company nomidan
// ishlovchi (act-as-company) token olinadi. Business switcher/aktiv kompaniya shu
// bilan ishlaydi — eski POST /main/v1/company/token o'rnini bosadi.
@RestController
@RequestMapping("/main/v1/act-as-company")
@RequiredArgsConstructor
public class ActAsCompanyController {

  private final ActAsCompanyService actAsCompanyService;

  @PostMapping
  public Mono<JwtTokenResponse> actAsCompany(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @RequestBody CompanyTokenRequest request) {
    return actAsCompanyService.issue(userPrincipal, request.companyId());
  }
}
