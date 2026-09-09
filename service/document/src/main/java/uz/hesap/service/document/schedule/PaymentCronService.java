package uz.hesap.service.document.schedule;

import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.TextModel;
import uz.hesap.service.common.util.message.FirebaseNotificationReply;
import uz.hesap.service.common.util.message.NotificationType;
import uz.hesap.service.document.util.Constants;
import uz.hesap.service.jms.JmsPublisher;

// Har kuni 09:00 da to'lov eslatmalarini yuboradi
@Log4j2
@Service
@RequiredArgsConstructor
public class PaymentCronService {

  private static final Set<Integer> REMINDER_DAYS = Set.of(0, 1, 2, 3, 5, 7);
  private static final String PS_TABLE = Constants.SCHEMA + "." + Constants.TABLE_PAYMENT_SCHEDULE;
  private static final String DOC_TABLE = Constants.SCHEMA + "." + Constants.TABLE_DOCUMENT;

  private final DatabaseClient databaseClient;
  private final JmsPublisher jmsPublisher;
  private final uz.hesap.service.document.webclient.UserServiceClient userServiceClient;
  private final uz.hesap.service.document.webclient.LogServiceClient logServiceClient;

  @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Tashkent")
  public void sendPaymentReminders() {
    log.info("Payment reminder cron started");
    // Audit: log servisga start yozuvni yaratamiz, id ni saqlab, har send'ni
    // success/fail counterga qo'shib boramiz. Yakunda finish yuboriladi.
    java.util.concurrent.atomic.AtomicInteger total = new java.util.concurrent.atomic.AtomicInteger(0);
    java.util.concurrent.atomic.AtomicInteger success = new java.util.concurrent.atomic.AtomicInteger(0);
    java.util.concurrent.atomic.AtomicInteger failed = new java.util.concurrent.atomic.AtomicInteger(0);

    logServiceClient
        .startPaymentReminder()
        .flatMap(
            logId ->
                findUpcomingPayments()
                    .doOnNext(r -> total.incrementAndGet())
                    .flatMap(
                        row ->
                            sendReminder(row)
                                .doOnSuccess(v -> success.incrementAndGet())
                                .onErrorResume(
                                    e -> {
                                      failed.incrementAndGet();
                                      log.error(
                                          "Reminder send failed for buyerIn=[{}]: {}",
                                          row.buyerIn(),
                                          e.getMessage());
                                      return Mono.empty();
                                    }))
                    .then(
                        logServiceClient.finishPaymentReminder(
                            logId, total.get(), success.get(), failed.get(), null))
                    .onErrorResume(
                        e ->
                            logServiceClient.finishPaymentReminder(
                                logId, total.get(), success.get(), failed.get(), e.getMessage())))
        .doOnSuccess(v -> log.info("Payment reminder cron completed"))
        .subscribe();
  }

  // to'lov muddati 8 kundan kam qolgan yoki o'tib ketgan paymentlarni olish
  private Flux<PaymentReminderRow> findUpcomingPayments() {
    String sql =
        "SELECT DISTINCT ON (d.buyer_in, ps.contract_id)"
            + " d.buyer_in AS buyer_in,"
            + " ps.contract_id AS document_id,"
            + " d.number AS doc_number,"
            + " EXTRACT(DAY FROM ps.contract_payment_date - NOW()::date)::int AS diff"
            + " FROM "
            + PS_TABLE
            + " ps JOIN "
            + DOC_TABLE
            + " d ON d.id = ps.contract_id"
            + " WHERE ps.deleted = false"
            + " AND ps.status != 'PAID'"
            + " AND EXTRACT(DAY FROM ps.contract_payment_date - NOW()::date) < 8"
            + " ORDER BY d.buyer_in, ps.contract_id, diff";

    return databaseClient
        .sql(sql)
        .map(
            (row, metadata) ->
                new PaymentReminderRow(
                    row.get("buyer_in", String.class),
                    row.get("document_id", UUID.class),
                    row.get("doc_number", String.class),
                    row.get("diff", Integer.class)))
        .all()
        .filter(r -> r.diff() < 0 || REMINDER_DAYS.contains(r.diff()));
  }

  private Mono<Void> sendReminder(PaymentReminderRow row) {
    TextModel title = getReminderTitle(row.diff(), row.docNumber());
    if (title.uz() == null) return Mono.empty();
    if (row.buyerIn() == null || row.buyerIn().isBlank()) return Mono.empty();

    // recipient UUID buyer PINFL'dan tiklanadi (taraflar PINFL'da saqlanadi)
    return userServiceClient
        .getUserByIn(row.buyerIn())
        .flatMap(
            buyer -> {
              FirebaseNotificationReply notification =
                  FirebaseNotificationReply.withoutToken(
                      row.documentId(), title, title, NotificationType.PAYMENT_REQUEST, buyer.id());
              log.debug(
                  "Sending reminder to buyerIn [{}], doc №{}, diff={}",
                  row.buyerIn(),
                  row.docNumber(),
                  row.diff());
              return jmsPublisher.publish(notification);
            })
        .onErrorResume(
            e -> {
              log.error(
                  "Failed to send reminder to buyerIn [{}]: {}", row.buyerIn(), e.getMessage());
              return Mono.empty();
            });
  }

  private TextModel getReminderTitle(int diff, String docNumber) {
    String num = docNumber != null ? docNumber : "—";
    if (diff < 0) {
      return new TextModel(
          "To'lov vaqti o'tib ketdi, Shartnoma №" + num,
          "Срок оплаты истек, Договор №" + num,
          "Payment deadline has passed, Contract №" + num);
    }
    return switch (diff) {
      case 0 ->
          new TextModel(
              "Bugun to'lovning oxirgi kuni, Shartnoma №" + num,
              "Сегодня последний день оплаты, Договор №" + num,
              "Today is the last day of payment, Contract №" + num);
      case 1 ->
          new TextModel(
              "Ertaga to'lovning oxirgi kuni, Shartnoma №" + num,
              "Завтра последний день оплаты, Договор №" + num,
              "Tomorrow is the last day of payment, Contract №" + num);
      case 2 ->
          new TextModel(
              "To'lov muddati tugashiga 2 kun qoldi, Shartnoma №" + num,
              "До окончания срока оплаты осталось 2 дня, Договор №" + num,
              "2 days left until payment deadline, Contract №" + num);
      case 3 ->
          new TextModel(
              "To'lov muddati tugashiga 3 kun qoldi, Shartnoma №" + num,
              "До окончания срока оплаты осталось 3 дня, Договор №" + num,
              "3 days left until payment deadline, Contract №" + num);
      case 5 ->
          new TextModel(
              "To'lov muddati tugashiga 5 kun qoldi, Shartnoma №" + num,
              "До окончания срока оплаты осталось 5 дней, Договор №" + num,
              "5 days left until payment deadline, Contract №" + num);
      case 7 ->
          new TextModel(
              "To'lov muddati tugashiga 7 kun qoldi, Shartnoma №" + num,
              "До окончания срока оплаты осталось 7 дней, Договор №" + num,
              "7 days left until payment deadline, Contract №" + num);
      default -> new TextModel(null, null, null);
    };
  }

  private record PaymentReminderRow(
      String buyerIn, UUID documentId, String docNumber, Integer diff) {}
}
