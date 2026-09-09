package uz.hesap.service.document.repository;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import uz.hesap.service.document.domain.enums.PaymentScheduleStatus;
import uz.hesap.service.document.domain.payment.PaymentScheduleRequestEntity;

@Repository
@RequiredArgsConstructor
public class CustomPaymentScheduleRequestRepositoryImpl
    implements CustomPaymentScheduleRequestRepository {

  private final R2dbcEntityTemplate r2dbcEntityTemplate;

  // JOIN document — entity template join qila olmaydi, DatabaseClient + converter.
  @Override
  public Flux<PaymentScheduleRequestEntity> findIncomingPending(UUID sellerUserId) {
    String sql =
        """
        SELECT r.* FROM document.payment_requests r
        JOIN document.contracts d ON d.id = r.contract_id
        WHERE d.seller_in = (SELECT COALESCE(u.pinfl, u.tin)
                             FROM "user"."user" u WHERE u.id = :userId)
          AND d.deleted = false
          AND r.deleted = false
          AND r.status = 'PENDING'
        ORDER BY r.created_date DESC
        """;
    return r2dbcEntityTemplate
        .getDatabaseClient()
        .sql(sql)
        .bind("userId", sellerUserId)
        .map(
            (row, meta) ->
                r2dbcEntityTemplate
                    .getConverter()
                    .read(PaymentScheduleRequestEntity.class, row, meta))
        .all();
  }

  @Override
  public Flux<PaymentScheduleRequestEntity> findRequests(
      UUID paymentScheduleId,
      String receiverIn,
      String fromIn,
      String toIn,
      List<PaymentScheduleStatus> statuses) {
    // To'lov so'rovi: yuboruvchi = qarzdor (buyer), qabul qiluvchi = haqdor (seller).
    // fromIn → d.buyer_in (men yuborgan), toIn/receiverIn → d.seller_in (menga kelgan).
    StringBuilder sql =
        new StringBuilder(
            "SELECT r.* FROM document.payment_requests r "
                + "JOIN document.contracts d ON d.id = r.contract_id "
                + "WHERE r.deleted = false AND d.deleted = false");
    if (paymentScheduleId != null) sql.append(" AND r.payment_id = :paymentId");
    if (receiverIn != null && !receiverIn.isBlank()) sql.append(" AND d.seller_in = :receiverIn");
    if (fromIn != null && !fromIn.isBlank()) sql.append(" AND d.buyer_in = :fromIn");
    if (toIn != null && !toIn.isBlank()) sql.append(" AND d.seller_in = :toIn");
    boolean hasStatuses = statuses != null && !statuses.isEmpty();
    if (hasStatuses) sql.append(" AND r.status IN (:statuses)");
    sql.append(" ORDER BY r.created_date DESC");

    DatabaseClient.GenericExecuteSpec spec =
        r2dbcEntityTemplate.getDatabaseClient().sql(sql.toString());
    if (paymentScheduleId != null) spec = spec.bind("paymentId", paymentScheduleId);
    if (receiverIn != null && !receiverIn.isBlank()) spec = spec.bind("receiverIn", receiverIn);
    if (fromIn != null && !fromIn.isBlank()) spec = spec.bind("fromIn", fromIn);
    if (toIn != null && !toIn.isBlank()) spec = spec.bind("toIn", toIn);
    if (hasStatuses) {
      spec = spec.bind("statuses", statuses.stream().map(Enum::name).toList());
    }
    return spec.map(
            (row, meta) ->
                r2dbcEntityTemplate
                    .getConverter()
                    .read(PaymentScheduleRequestEntity.class, row, meta))
        .all();
  }

  @Override
  public Flux<PaymentScheduleRequestEntity> findDelayRequests(
      String buyerIn,
      String sellerIn,
      String fromIn,
      String toIn,
      UUID contractId,
      UUID paymentScheduleId,
      List<PaymentScheduleStatus> statuses) {
    // Kechiktirish: yuboruvchi = qarzdor (buyer), tasdiqlovchi = haqdor (seller).
    // fromIn → d.buyer_in (men yuborgan), toIn → d.seller_in (menga kelgan).
    StringBuilder sql =
        new StringBuilder(
            "SELECT r.* FROM document.delay_requests r "
                + "JOIN document.contracts d ON d.id = r.contract_id "
                + "WHERE r.deleted = false AND d.deleted = false");
    if (buyerIn != null && !buyerIn.isBlank()) sql.append(" AND d.buyer_in = :buyerIn");
    if (sellerIn != null && !sellerIn.isBlank()) sql.append(" AND d.seller_in = :sellerIn");
    if (fromIn != null && !fromIn.isBlank()) sql.append(" AND d.buyer_in = :fromIn");
    if (toIn != null && !toIn.isBlank()) sql.append(" AND d.seller_in = :toIn");
    if (contractId != null) sql.append(" AND r.contract_id = :contractId");
    if (paymentScheduleId != null) sql.append(" AND r.payment_id = :paymentId");
    boolean hasStatuses = statuses != null && !statuses.isEmpty();
    if (hasStatuses) sql.append(" AND r.status IN (:statuses)");
    sql.append(" ORDER BY r.created_date DESC");

    DatabaseClient.GenericExecuteSpec spec =
        r2dbcEntityTemplate.getDatabaseClient().sql(sql.toString());
    if (buyerIn != null && !buyerIn.isBlank()) spec = spec.bind("buyerIn", buyerIn);
    if (sellerIn != null && !sellerIn.isBlank()) spec = spec.bind("sellerIn", sellerIn);
    if (fromIn != null && !fromIn.isBlank()) spec = spec.bind("fromIn", fromIn);
    if (toIn != null && !toIn.isBlank()) spec = spec.bind("toIn", toIn);
    if (contractId != null) spec = spec.bind("contractId", contractId);
    if (paymentScheduleId != null) spec = spec.bind("paymentId", paymentScheduleId);
    if (hasStatuses) {
      spec = spec.bind("statuses", statuses.stream().map(Enum::name).toList());
    }
    return spec.map(
            (row, meta) ->
                r2dbcEntityTemplate
                    .getConverter()
                    .read(PaymentScheduleRequestEntity.class, row, meta))
        .all();
  }

  @Override
  public Flux<uz.hesap.service.document.model.response.DelayRequestListResponse>
      findDelayRequestsEnriched(
          String buyerIn,
          String sellerIn,
          String fromIn,
          String toIn,
          UUID contractId,
          UUID paymentScheduleId,
          List<PaymentScheduleStatus> statuses) {
    // Ro'yxat uchun boyitma: shartnoma raqami (contracts.number), so'rovchi ism-sharifi
    // (kechiktirishni faqat buyer yuboradi — buyer_in bo'yicha user.user'dan, dual-register
    // holatida CLIENT afzal), nechanchi to'lov (muddat+id bo'yicha tartib) va jami soni.
    StringBuilder sql =
        new StringBuilder(
            """
            SELECT r.*, d.number AS contract_number,
              (SELECT TRIM(CONCAT(u.last_name, ' ', u.first_name, ' ', COALESCE(u.mid_name, '')))
                 FROM "user"."user" u
                 WHERE COALESCE(u.pinfl, u.tin) = r.buyer_in AND u.deleted = false
                 ORDER BY CASE WHEN u.type = 'CLIENT' THEN 0 ELSE 1 END
                 LIMIT 1) AS requester_name,
              (SELECT COUNT(*) + 1 FROM document.payments p2
                 WHERE p2.contract_id = r.contract_id AND p2.deleted = false
                   AND (p2.contract_payment_date < p.contract_payment_date
                        OR (p2.contract_payment_date = p.contract_payment_date AND p2.id < p.id)))
                AS payment_order,
              (SELECT COUNT(*) FROM document.payments p3
                 WHERE p3.contract_id = r.contract_id AND p3.deleted = false) AS payments_total
            FROM document.delay_requests r
            JOIN document.contracts d ON d.id = r.contract_id
            LEFT JOIN document.payments p ON p.id = r.payment_id
            WHERE r.deleted = false AND d.deleted = false
            """);
    if (buyerIn != null && !buyerIn.isBlank()) sql.append(" AND d.buyer_in = :buyerIn");
    if (sellerIn != null && !sellerIn.isBlank()) sql.append(" AND d.seller_in = :sellerIn");
    if (fromIn != null && !fromIn.isBlank()) sql.append(" AND d.buyer_in = :fromIn");
    if (toIn != null && !toIn.isBlank()) sql.append(" AND d.seller_in = :toIn");
    if (contractId != null) sql.append(" AND r.contract_id = :contractId");
    if (paymentScheduleId != null) sql.append(" AND r.payment_id = :paymentId");
    boolean hasStatuses = statuses != null && !statuses.isEmpty();
    if (hasStatuses) sql.append(" AND r.status IN (:statuses)");
    sql.append(" ORDER BY r.created_date DESC");

    DatabaseClient.GenericExecuteSpec spec =
        r2dbcEntityTemplate.getDatabaseClient().sql(sql.toString());
    if (buyerIn != null && !buyerIn.isBlank()) spec = spec.bind("buyerIn", buyerIn);
    if (sellerIn != null && !sellerIn.isBlank()) spec = spec.bind("sellerIn", sellerIn);
    if (fromIn != null && !fromIn.isBlank()) spec = spec.bind("fromIn", fromIn);
    if (toIn != null && !toIn.isBlank()) spec = spec.bind("toIn", toIn);
    if (contractId != null) spec = spec.bind("contractId", contractId);
    if (paymentScheduleId != null) spec = spec.bind("paymentId", paymentScheduleId);
    if (hasStatuses) {
      spec = spec.bind("statuses", statuses.stream().map(Enum::name).toList());
    }
    return spec.map(
            (row, meta) ->
                new uz.hesap.service.document.model.response.DelayRequestListResponse(
                    row.get("id", UUID.class),
                    row.get("buyer_in", String.class),
                    row.get("seller_in", String.class),
                    // kechiktirish so'rovini har doim buyer yaratadi
                    row.get("buyer_in", String.class),
                    row.get("contract_id", UUID.class),
                    parseStatus(row.get("status", String.class)),
                    row.get("payment_id", UUID.class),
                    row.get("amount", Double.class),
                    parseCurrency(row.get("currency", String.class)),
                    row.get("payment_date", java.time.Instant.class),
                    row.get("note", String.class),
                    row.get("contract_number", String.class),
                    row.get("requester_name", String.class),
                    intOrNull(row.get("payment_order", Long.class)),
                    intOrNull(row.get("payments_total", Long.class)),
                    row.get("created_date", java.time.Instant.class),
                    row.get("last_modified_date", java.time.Instant.class)))
        .all();
  }

  private static PaymentScheduleStatus parseStatus(String s) {
    try {
      return s == null ? null : PaymentScheduleStatus.valueOf(s);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private static uz.hesap.service.document.domain.enums.Currency parseCurrency(String s) {
    try {
      return s == null ? null : uz.hesap.service.document.domain.enums.Currency.valueOf(s);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private static Integer intOrNull(Long v) {
    return v == null ? null : v.intValue();
  }
}
