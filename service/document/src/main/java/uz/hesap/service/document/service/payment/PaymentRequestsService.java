package uz.hesap.service.document.service.payment;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.UserPrincipal;
import uz.hesap.service.common.util.UserResponse;
import uz.hesap.service.document.domain.enums.PaymentScheduleStatus;
import uz.hesap.service.document.model.request.PaymentScheduleRequestRequest;
import uz.hesap.service.document.model.response.PaymentScheduleRequestResponse;

/** PaymentRequestsController doirasi — to'lov so'rovlari (yuborish/tasdiq/rad/ro'yxat). */
@Service
@RequiredArgsConstructor
public class PaymentRequestsService {

  private final PaymentScheduleQueryService queryService;
  private final PaymentScheduleCommandService commandService;

  public Mono<Void> create(UserResponse user, PaymentScheduleRequestRequest request) {
    return commandService.createPaymentRequest(user, request);
  }

  public Mono<Void> approve(UUID requestId, UserPrincipal userPrincipal) {
    return commandService.approvePaymentRequest(requestId, userPrincipal);
  }

  public Mono<Void> reject(UUID requestId) {
    return commandService.rejectPaymentRequest(requestId);
  }

  public Mono<Void> cancel(UUID requestId) {
    return commandService.cancelPaymentRequest(requestId);
  }

  public Flux<PaymentScheduleRequestResponse> getRequests(
      UUID paymentId,
      String receiverIn,
      String fromIn,
      String toIn,
      List<PaymentScheduleStatus> statuses) {
    return queryService.getRequests(paymentId, receiverIn, fromIn, toIn, statuses);
  }
}
