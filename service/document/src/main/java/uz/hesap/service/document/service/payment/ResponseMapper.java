package uz.hesap.service.document.service.payment;

import java.util.Map;
import java.util.UUID;
import uz.hesap.service.common.util.UserBasicResponse;
import uz.hesap.service.document.domain.document.DocumentEntity;

@FunctionalInterface
public interface ResponseMapper<E, R> {
  // usersMap — taraflar PINFL/STIR (buyer_in/seller_in) bo'yicha.
  R map(
      E entity, Map<UUID, DocumentEntity> docsMap, Map<String, UserBasicResponse> usersMap);
}
