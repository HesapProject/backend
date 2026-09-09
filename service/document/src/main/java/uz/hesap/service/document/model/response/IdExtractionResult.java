package uz.hesap.service.document.model.response;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import uz.hesap.service.document.domain.payment.PaidScheduleEntity;
import uz.hesap.service.document.domain.payment.PaymentEntity;
import uz.hesap.service.document.domain.payment.PaymentScheduleRequestEntity;

public record IdExtractionResult(Set<UUID> users) {

  /** PaymentEntity endi buyer/seller user UUID saqlamaydi (buyer_in/seller_in = PINFL) —
   * user'lar shartnoma orqali hal qilinadi, bu yerda bo'sh. */
  public static IdExtractionResult fromSchedules(List<PaymentEntity> list) {
    return new IdExtractionResult(Set.of());
  }

  /** PaymentScheduleRequestEntity endi user UUID saqlamaydi (buyer_in/seller_in = PINFL). */
  public static IdExtractionResult fromRequests(List<PaymentScheduleRequestEntity> list) {
    return new IdExtractionResult(Set.of());
  }

  /** PaidScheduleEntity ham PINFL saqlaydi — user'lar shartnoma orqali, bu yerda bo'sh. */
  public static IdExtractionResult fromPaidSchedules(List<PaidScheduleEntity> list) {
    return new IdExtractionResult(Set.of());
  }

  private static <E> IdExtractionResult extract(
      List<E> list, Function<E, Stream<UUID>> userExtractor) {
    Set<UUID> users =
        list.stream().flatMap(userExtractor).filter(Objects::nonNull).collect(Collectors.toSet());
    return new IdExtractionResult(users);
  }
}
