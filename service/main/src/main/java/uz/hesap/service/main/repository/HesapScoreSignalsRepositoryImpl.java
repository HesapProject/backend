package uz.hesap.service.main.repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple4;
import uz.hesap.service.main.model.HesapScoreSignals;

// Cross-schema (document.*) xom signal o'qish — bitta hesap DB (HTTP hop yo'q).
// cutoff String CAST bilan bind qilinadi (Instant→timestamp binding tuzog'idan qochish).
@Repository
@RequiredArgsConstructor
public class HesapScoreSignalsRepositoryImpl implements HesapScoreSignalsRepository {

  private final DatabaseClient db;

  // S1 — to'lov punktualligi (buyer_in), yosh-decay og'irlikli. Faqat ACTIVE/COMPLETED
  // shartnomalar (draft/void chiqadi), cutoff'dan keyingi, deleted=false.
  private static final String PAYMENTS_SQL =
      """
      SELECT
        COALESCE(SUM(CASE WHEN paid_at IS NOT NULL
              AND date_trunc('day', paid_at) < date_trunc('day', contract_payment_date) THEN w END), 0) AS w_early,
        COALESCE(SUM(CASE WHEN paid_at IS NOT NULL
              AND date_trunc('day', paid_at) = date_trunc('day', contract_payment_date) THEN w END), 0) AS w_ontime,
        COALESCE(SUM(CASE WHEN paid_at IS NOT NULL
              AND date_trunc('day', paid_at) > date_trunc('day', contract_payment_date)
              AND paid_at <= contract_payment_date + INTERVAL '30 days' THEN w END), 0) AS w_late,
        COALESCE(SUM(CASE WHEN paid_at IS NOT NULL
              AND paid_at > contract_payment_date + INTERVAL '30 days' THEN w END), 0) AS w_verylate,
        COALESCE(SUM(CASE WHEN paid_at IS NULL AND contract_payment_date < NOW() THEN w END), 0) AS w_unpaid,
        COUNT(*) FILTER (WHERE paid_at IS NOT NULL OR contract_payment_date < NOW()) AS n_matured,
        COUNT(*) FILTER (WHERE paid_at IS NULL AND contract_payment_date < NOW()) AS unpaid_overdue,
        COALESCE(MAX(EXTRACT(EPOCH FROM (NOW() - contract_payment_date)) / 86400)
              FILTER (WHERE paid_at IS NULL AND contract_payment_date < NOW()), 0) AS max_overdue_days
      FROM (
        SELECT p.paid_at, p.contract_payment_date,
               power(0.5, EXTRACT(EPOCH FROM (NOW() - p.contract_payment_date)) / :halfLifeSec) AS w
        FROM document.payments p
        WHERE p.deleted = false
          AND p.contract_payment_date >= CAST(:cutoff AS timestamp)
          AND p.contract_id IN (
            SELECT id FROM document.contracts
            WHERE buyer_in = :userIn AND deleted = false AND status IN ('ACTIVE','COMPLETED')
          )
      ) t
      """;

  // S2/S4 — shartnoma yakunlanishi + hajm/tenure/counterparties (ikkala taraf).
  private static final String CONTRACTS_SQL =
      """
      SELECT
        COUNT(*) FILTER (WHERE status = 'COMPLETED') AS completed,
        COUNT(*) FILTER (WHERE status = 'REJECTED')  AS rejected,
        COUNT(*) FILTER (WHERE status = 'CANCELLED') AS cancelled,
        COUNT(*) FILTER (WHERE status = 'ACTIVE')    AS active,
        COUNT(DISTINCT CASE WHEN buyer_in = :userIn THEN seller_in ELSE buyer_in END) AS counterparties,
        COALESCE(EXTRACT(EPOCH FROM (NOW() - MIN(created_date))) / 2629800, 0) AS tenure_months,
        COUNT(*) AS total_contracts
      FROM document.contracts
      WHERE (buyer_in = :userIn OR seller_in = :userIn) AND deleted = false
        AND created_date >= CAST(:cutoff AS timestamp)
      """;

  // S3/S5 — delinquency/friction (userga qarshi): talabnoma, da'vo (to_in), bekor, kechiktirish.
  private static final String SIGNALS_SQL =
      """
      SELECT
        (SELECT COUNT(*) FROM document.notices        WHERE buyer_in = :userIn AND deleted = false) AS notices_against,
        (SELECT COUNT(*) FROM document.claims         WHERE to_in    = :userIn AND deleted = false) AS claims_against,
        (SELECT COUNT(*) FROM document.cancel_requests WHERE requester_in = :userIn AND status = 'APPROVED' AND deleted = false) AS cancels_approved,
        (SELECT COUNT(*) FROM document.delay_requests WHERE buyer_in = :userIn AND deleted = false) AS delays
      """;

  // Anti-gaming q uchun — har counterparty bilan shartnoma soni.
  private static final String PAIRS_SQL =
      """
      SELECT COUNT(*) AS cnt
      FROM document.contracts
      WHERE (buyer_in = :userIn OR seller_in = :userIn) AND deleted = false
        AND created_date >= CAST(:cutoff AS timestamp)
      GROUP BY (CASE WHEN buyer_in = :userIn THEN seller_in ELSE buyer_in END)
      """;

  private static final String ACTIVE_PARTIES_SQL =
      """
      SELECT DISTINCT party FROM (
        SELECT buyer_in  AS party FROM document.contracts WHERE status = 'ACTIVE' AND deleted = false AND buyer_in  IS NOT NULL
        UNION
        SELECT seller_in AS party FROM document.contracts WHERE status = 'ACTIVE' AND deleted = false AND seller_in IS NOT NULL
      ) x
      """;

  @Override
  public Mono<HesapScoreSignals> getSignals(String userIn, Instant cutoff, double halfLifeDays) {
    String cutoffStr = LocalDateTime.ofInstant(cutoff, ZoneOffset.UTC).toString();
    double halfLifeSec = halfLifeDays * 86400.0;

    Mono<double[]> payments =
        db.sql(PAYMENTS_SQL)
            .bind("userIn", userIn)
            .bind("cutoff", cutoffStr)
            .bind("halfLifeSec", halfLifeSec)
            .map(
                (row, meta) ->
                    new double[] {
                      d(row.get("w_early")), d(row.get("w_ontime")), d(row.get("w_late")),
                      d(row.get("w_verylate")), d(row.get("w_unpaid")), (double) l(row.get("n_matured")),
                      (double) l(row.get("unpaid_overdue")), d(row.get("max_overdue_days"))
                    })
            .one()
            .defaultIfEmpty(new double[8]);

    Mono<long[]> contracts =
        db.sql(CONTRACTS_SQL)
            .bind("userIn", userIn)
            .bind("cutoff", cutoffStr)
            .map(
                (row, meta) ->
                    new long[] {
                      l(row.get("completed")), l(row.get("rejected")), l(row.get("cancelled")),
                      l(row.get("active")), l(row.get("counterparties")),
                      (long) d(row.get("tenure_months")), l(row.get("total_contracts"))
                    })
            .one()
            .defaultIfEmpty(new long[7]);

    Mono<long[]> extra =
        db.sql(SIGNALS_SQL)
            .bind("userIn", userIn)
            .map(
                (row, meta) ->
                    new long[] {
                      l(row.get("notices_against")), l(row.get("claims_against")),
                      l(row.get("cancels_approved")), l(row.get("delays"))
                    })
            .one()
            .defaultIfEmpty(new long[4]);

    Mono<List<Long>> pairs =
        db.sql(PAIRS_SQL)
            .bind("userIn", userIn)
            .bind("cutoff", cutoffStr)
            .map((row, meta) -> l(row.get("cnt")))
            .all()
            .collectList();

    return Mono.zip(payments, contracts, extra, pairs).map(this::assemble);
  }

  private HesapScoreSignals assemble(Tuple4<double[], long[], long[], List<Long>> t) {
    double[] p = t.getT1();
    long[] c = t.getT2();
    long[] e = t.getT3();
    List<Long> pairs = t.getT4();
    double tenureMonths = c.length > 5 ? c[5] : 0;
    return new HesapScoreSignals(
        p[0], p[1], p[2], p[3], p[4],
        (long) p[5], (long) p[6], p[7],
        c[0], c[1], c[2], c[3], c[4], tenureMonths, c[6],
        e[0], e[1], e[2], e[3],
        pairs);
  }

  @Override
  public Flux<String> activePartyIdentifiers() {
    return db.sql(ACTIVE_PARTIES_SQL).map((row, meta) -> row.get("party", String.class)).all();
  }

  private static double d(Object v) {
    return v == null ? 0.0 : ((Number) v).doubleValue();
  }

  private static long l(Object v) {
    return v == null ? 0L : ((Number) v).longValue();
  }
}
