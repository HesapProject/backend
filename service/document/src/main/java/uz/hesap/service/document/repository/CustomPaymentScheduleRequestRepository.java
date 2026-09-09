package uz.hesap.service.document.repository;

import java.util.List;
import java.util.UUID;
import reactor.core.publisher.Flux;
import uz.hesap.service.document.domain.enums.PaymentScheduleStatus;
import uz.hesap.service.document.domain.payment.PaymentScheduleRequestEntity;

public interface CustomPaymentScheduleRequestRepository {
  // User seller (haqdor) bo'lgan hujjatlardagi PENDING PAYMENT so'rovlari
  // (So'rovlarim sahifasi uchun).
  Flux<PaymentScheduleRequestEntity> findIncomingPending(UUID sellerUserId);

  // Filtrlangan: paymentScheduleId, receiverIn (hujjat seller_in — kimga yuborilgan),
  // statuses. Har biri ixtiyoriy (null/bo'sh → o'sha filtr qo'llanmaydi).
  // fromIn → d.buyer_in (yuboruvchi), toIn → d.seller_in (qabul qiluvchi).
  Flux<PaymentScheduleRequestEntity> findRequests(
      UUID paymentScheduleId,
      String receiverIn,
      String fromIn,
      String toIn,
      List<PaymentScheduleStatus> statuses);

  // Kechiktirish so'rovlari (type=DELAY) — buyerIn/sellerIn, fromIn/toIn (hujjat),
  // contractId, paymentId, statuses bo'yicha filtr (har biri ixtiyoriy).
  Flux<PaymentScheduleRequestEntity> findDelayRequests(
      String buyerIn,
      String sellerIn,
      String fromIn,
      String toIn,
      UUID contractId,
      UUID paymentScheduleId,
      List<PaymentScheduleStatus> statuses);

  // Kechiktirish so'rovlari — ro'yxat uchun boyitilgan: shartnoma raqami,
  // so'rovchi (buyer) ism-sharifi, nechanchi to'lov / jami to'lovlar soni.
  Flux<uz.hesap.service.document.model.response.DelayRequestListResponse>
      findDelayRequestsEnriched(
          String buyerIn,
          String sellerIn,
          String fromIn,
          String toIn,
          UUID contractId,
          UUID paymentScheduleId,
          List<PaymentScheduleStatus> statuses);
}
