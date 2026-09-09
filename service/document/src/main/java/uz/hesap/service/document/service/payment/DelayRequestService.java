package uz.hesap.service.document.service.payment;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.UserPrincipal;
import uz.hesap.service.document.domain.enums.PaymentScheduleStatus;
import uz.hesap.service.document.model.request.DelayRequestCreateRequest;
import uz.hesap.service.document.model.response.DelayRequestListResponse;

/** DelayRequestController doirasi — kechiktirish so'rovlari (yuborish/tasdiq/rad/ro'yxat). */
@Service
@RequiredArgsConstructor
public class DelayRequestService {

  private final PaymentScheduleQueryService queryService;
  private final PaymentScheduleCommandService commandService;

  public Mono<Void> create(UserPrincipal userPrincipal, DelayRequestCreateRequest request) {
    return commandService.createDelayRequest(userPrincipal, request);
  }

  public Mono<Void> approve(UserPrincipal userPrincipal, UUID delayId) {
    return commandService.approveDelayRequest(userPrincipal, delayId);
  }

  public Mono<Void> reject(UserPrincipal userPrincipal, UUID delayId) {
    return commandService.rejectDelayRequest(userPrincipal, delayId);
  }

  public Mono<Void> cancel(UUID delayId) {
    return commandService.cancelDelayRequest(delayId);
  }

  public Flux<DelayRequestListResponse> getDelayRequests(
      String buyerIn,
      String sellerIn,
      String fromIn,
      String toIn,
      UUID contractId,
      UUID paymentId,
      List<PaymentScheduleStatus> statuses) {
    return queryService.getDelayRequests(buyerIn, sellerIn, fromIn, toIn, contractId, paymentId, statuses);
  }
}
