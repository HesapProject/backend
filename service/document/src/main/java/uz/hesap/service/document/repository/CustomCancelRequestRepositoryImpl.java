package uz.hesap.service.document.repository;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import uz.hesap.service.document.domain.document.CancelRequestEntity;
import uz.hesap.service.document.domain.enums.CancelRequestStatus;

@Repository
@RequiredArgsConstructor
public class CustomCancelRequestRepositoryImpl implements CustomCancelRequestRepository {

  private final R2dbcEntityTemplate template;

  // cancel_requests buyer_in/seller_in/contract_id'ni to'g'ridan-to'g'ri saqlaydi — JOIN'siz.
  private static final String BASE =
      "SELECT * FROM document.cancel_requests c WHERE c.deleted = false";

  @Override
  public Flux<CancelRequestEntity> findFiltered(
      String buyerIn,
      String sellerIn,
      String fromIn,
      String toIn,
      UUID contractId,
      List<CancelRequestStatus> statuses) {
    // Bekor so'rovini istalgan taraf yuborishi mumkin — `requester_in` kim yuborganini saqlaydi.
    // fromIn → men yuborgan (requester_in). toIn → menga kelgan (men tarafman, lekin so'rovchi emas).
    StringBuilder sql = new StringBuilder(BASE);
    if (buyerIn != null && !buyerIn.isBlank()) sql.append(" AND c.buyer_in = :buyerIn");
    if (sellerIn != null && !sellerIn.isBlank()) sql.append(" AND c.seller_in = :sellerIn");
    if (fromIn != null && !fromIn.isBlank()) sql.append(" AND c.requester_in = :fromIn");
    if (toIn != null && !toIn.isBlank())
      sql.append(
          " AND (c.buyer_in = :toIn OR c.seller_in = :toIn)"
              + " AND COALESCE(c.requester_in, '') <> :toIn");
    if (contractId != null) sql.append(" AND c.contract_id = :contractId");
    boolean hasStatuses = statuses != null && !statuses.isEmpty();
    if (hasStatuses) sql.append(" AND c.status IN (:statuses)");
    sql.append(" ORDER BY c.created_date DESC");

    DatabaseClient.GenericExecuteSpec spec =
        template.getDatabaseClient().sql(sql.toString());
    if (buyerIn != null && !buyerIn.isBlank()) spec = spec.bind("buyerIn", buyerIn);
    if (sellerIn != null && !sellerIn.isBlank()) spec = spec.bind("sellerIn", sellerIn);
    if (fromIn != null && !fromIn.isBlank()) spec = spec.bind("fromIn", fromIn);
    if (toIn != null && !toIn.isBlank()) spec = spec.bind("toIn", toIn);
    if (contractId != null) spec = spec.bind("contractId", contractId);
    if (hasStatuses) spec = spec.bind("statuses", statuses.stream().map(Enum::name).toList());

    return spec.map((row, meta) -> template.getConverter().read(CancelRequestEntity.class, row, meta))
        .all();
  }
}
