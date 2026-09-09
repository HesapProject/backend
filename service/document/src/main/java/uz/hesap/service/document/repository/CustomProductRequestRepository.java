package uz.hesap.service.document.repository;

import java.util.List;
import java.util.UUID;
import reactor.core.publisher.Flux;
import uz.hesap.service.document.domain.document.ProductRequestEntity;
import uz.hesap.service.document.domain.enums.ProductRequestStatus;

public interface CustomProductRequestRepository {

  // Filtrlangan ro'yxat — barcha parametrlar optional.
  // fromIn → requester_in (men yuborgan), toIn → men taraf, lekin so'rovchi emas (menga kelgan).
  Flux<ProductRequestEntity> findFiltered(
      String buyerIn,
      String sellerIn,
      String fromIn,
      String toIn,
      UUID contractId,
      List<ProductRequestStatus> statuses);
}
