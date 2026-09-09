package uz.hesap.service.integration.service.payment;

import static uz.hesap.service.integration.model.payme.OrderTransaction.*;
import static uz.hesap.service.integration.model.payme.Receive.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.exception.InvalidArgumentException;
import uz.hesap.service.common.util.message.PaymeLogReply;
import uz.hesap.service.integration.domain.*;
import uz.hesap.service.integration.domain.enums.BalanceType;
import uz.hesap.service.integration.domain.enums.BillingType;
import uz.hesap.service.integration.domain.enums.TransactionType;
import uz.hesap.service.integration.model.mapper.PaymeMapper;
import uz.hesap.service.integration.model.payme.*;
import uz.hesap.service.integration.model.payme.Error;
import uz.hesap.service.integration.model.payme.ErrorResult;
import uz.hesap.service.integration.repository.BalanceRepository;
import uz.hesap.service.integration.repository.PaymeRepository;
import uz.hesap.service.integration.repository.TransactionRepository;
import uz.hesap.service.integration.service.PaymeSettingService;
import uz.hesap.service.integration.service.client.UserServiceClient;
import uz.hesap.service.integration.util.PaymeInterface;
import uz.hesap.service.jms.JmsPublisher;

@Log4j2
@Service
public class PaymeService {

  private final PaymeRepository paymeRepository;
  private final PaymeSettingService paymeSettingService;
  private final BalanceHelper balanceHelper;
  private final TransactionRepository transactionRepository;
  private final BalanceRepository balanceRepository;
  private final ObjectMapper objectMapper;
  private static final Long time_expired = 43_200_000L;
  // Payme yopilmagan tranzaksiyani cheksiz qayta so'raydi (bir xil xato javobi).
  // Bir xil (method + txId + xato) uchun logni shu oraliqda faqat bir marta yozamiz —
  // Payme'ga qaytadigan javob o'zgarmaydi, faqat log/RMQ toshqini to'xtaydi.
  private static final long LOG_DEDUP_WINDOW_MS = 3_600_000L;
  private final Map<String, Long> lastErrorLoggedAt = new ConcurrentHashMap<>();
  private final UserServiceClient userServiceClient;
  private final JmsPublisher jmsPublisher;
  private final PaymentRecorder paymentRecorder;
  private final PackageOrderService packageOrderService;

  public PaymeService(
      PaymeRepository paymeRepository,
      final PaymeSettingService paymeSettingService,
      final BalanceHelper balanceHelper,
      final TransactionRepository transactionRepository,
      final ObjectMapper objectMapper,
      final BalanceRepository balanceRepository1,
      final UserServiceClient userServiceClient,
      final JmsPublisher jmsPublisher,
      final PaymentRecorder paymentRecorder,
      final PackageOrderService packageOrderService) {
    this.paymeRepository = paymeRepository;
    this.paymeSettingService = paymeSettingService;
    this.balanceHelper = balanceHelper;
    this.transactionRepository = transactionRepository;
    this.objectMapper = objectMapper;
    this.balanceRepository = balanceRepository1;
    this.userServiceClient = userServiceClient;
    this.jmsPublisher = jmsPublisher;
    this.paymentRecorder = paymentRecorder;
    this.packageOrderService = packageOrderService;
  }

  public Mono<PaymeInterface> processWithMerchantId(String authorizationHeader, Receive receive) {
    log.debug("Payme : {}  {} ", authorizationHeader, receive);
    if (authorizationHeader == null || authorizationHeader.isEmpty()) {
      log.error("Authorization header error");
      return Mono.just(new ErrorResult(new Error(-32504, Message.WRONG_HEADERS), receive.id()));
    }

    String base64Credentials = authorizationHeader.substring("Basic ".length()).trim();
    byte[] decodedBytes = Base64.getDecoder().decode(base64Credentials);
    String decodedString = new String(decodedBytes, StandardCharsets.UTF_8);
    String[] decodedCredentials = decodedString.split(":");
    String login = decodedCredentials.length > 0 ? decodedCredentials[0] : "";
    String password = decodedCredentials.length > 1 ? decodedCredentials[1] : "";

    if (!"Paycom".equals(login)) {
      log.error("Authorization header error | payment not found");
      return Mono.just(new ErrorResult(new Error(-32504, Message.WRONG_HEADERS), receive.id()));
    }

    HashMap<String, Object> params = receive.params();

    Object account = params.get("account");
    log.debug(account);

    Mono<PaymeInterface> result =
        switch (receive.method()) {
          case CHECK_PERFORM_TRANSACTION ->
              checkPerform(account, (Integer) params.get("amount"), receive.id(), password);
          case CREATE_TRANSACTION ->
              createTransaction(
                  account,
                  receive.id(),
                  (Integer) params.get("amount"),
                  (String) params.get("id"),
                  (Long) params.get("time"),
                  password);
          case PERFORM_TRANSACTION ->
              performTransaction((String) params.get("id"), receive.id(), password);
          case CANCEL_TRANSACTION ->
              cancelTransaction(
                  (String) params.get("id"), (Integer) params.get("reason"), receive.id(), password);
          case CHECK_TRANSACTION -> checkTransaction((String) params.get("id"), receive.id());
          case GET_STATEMENT -> getStatement((Long) params.get("from"), (Long) params.get("to"));
          default -> Mono.error(new IllegalStateException("Unexpected value: " + receive.method()));
        };

    final UUID accountId = extractCompanyId(account);
    final String txId = (String) params.get("id");
    final String type = receive.method() == null ? "UNKNOWN" : receive.method();
    // Loglarda "Mijoz"ni ko'rsatish uchun foydalanuvchi id'sini aniqlaymiz. Fire-and-forget —
    // javob oqimiga ta'sir qilmaydi (to'lov mantig'i o'zgarmaydi).
    return result
        .doOnNext(
            res ->
                resolveLogUserId(accountId, txId)
                    .map(Optional::of)
                    .defaultIfEmpty(Optional.empty())
                    .subscribe(
                        uid -> {
                          boolean isError = res instanceof ErrorResult;
                          String errorMessage = extractErrorMessage(res);
                          if (isError && isDuplicateError(type, txId, errorMessage)) return;
                          sendPaymeLog(
                              uid.orElse(null),
                              null,
                              type,
                              isError ? "ERROR" : "SUCCESS",
                              errorMessage,
                              toJson(receive),
                              toJson(res));
                        }))
        .doOnError(
            error ->
                resolveLogUserId(accountId, txId)
                    .map(Optional::of)
                    .defaultIfEmpty(Optional.empty())
                    .subscribe(
                        uid -> {
                          if (isDuplicateError(type, txId, error.getMessage())) return;
                          sendPaymeLog(
                              uid.orElse(null), null, type, "ERROR", error.getMessage(),
                              toJson(receive), null);
                        }));
  }

  // Log uchun foydalanuvchi id'si: account.id (CheckPerform/CreateTransaction) yoki
  // tranzaksiya uuid'si (Perform/Cancel/Check — account bo'lmaydi). Hech qachon errormaydi.
  private Mono<UUID> resolveLogUserId(final UUID accountId, final String txId) {
    if (accountId != null) return Mono.just(accountId);
    if (txId != null) {
      return paymeRepository
          .findByPaycomId(txId)
          .map(PaymeTransactionEntity::getUuid)
          .onErrorResume(e -> Mono.empty());
    }
    return Mono.empty();
  }

  // ErrorResult'dan o'qiladigan xato matnini ajratadi (logga yozish uchun).
  private String extractErrorMessage(final PaymeInterface res) {
    if (res instanceof ErrorResult err && err.error() != null && err.error().message() != null) {
      return err.error().message().en();
    }
    return null;
  }

  // account JSON'idan companyId ni best-effort ajratadi (ba'zi methodlarda account yo'q).
  private UUID extractCompanyId(final Object account) {
    if (account == null) return null;
    try {
      Account a = objectMapper.convertValue(account, Account.class);
      return a == null ? null : a.companyId();
    } catch (Exception e) {
      return null;
    }
  }

  public Mono<PaymeInterface> checkPerform(
      final Object account, final Integer amount, final Long transactionId, final String password) {
    log.debug(
        "CheckPerform: account - {}, amount - {}, account - {}", account, amount, transactionId);

    try {
      Account account1 = objectMapper.convertValue(account, Account.class);
      return balanceHelper
          .getBalanceEntity(account1.companyId(), BalanceType.SUMMA)
          .flatMap(
              balance ->
                  paymeSettingService
                      .getCurrent()
                      .flatMap(
                          payment -> {
                            if (!Objects.equals(payment.getSecret(), password)) {
                              log.error("Authorization header error | payment not found");
                              return Mono.just(
                                  new ErrorResult(
                                      new Error(-32504, Message.WRONG_HEADERS), transactionId));
                            }

                            if (amount < 50000) {
                              log.error("Amount is less than 500.00");
                              return Mono.just(
                                  new ErrorResult(
                                      new Error(-31001, Message.WRONG_AMOUNT, "amount"),
                                      transactionId));
                            }

                            HashMap<String, Object> info = new HashMap<>();

                            info.put("price", amount / 100);
                            return Mono.just(
                                new CheckPerformResult(new ChResult(Boolean.TRUE, info, null)));
                          })
                      .switchIfEmpty(
                          Mono.just(
                              new ErrorResult(
                                  new Error(-32504, Message.WRONG_HEADERS), transactionId))));

    } catch (Exception e) {
      log.error(e.getMessage());
      return Mono.just(
          new ErrorResult(new Error(-31050, Message.UNABLE_TO_COMPLETE_OPERATION), transactionId));
    }
  }

  public Mono<PaymeInterface> createTransaction(
      final Object account,
      final Long transactionId,
      final Integer amount,
      final String paycomId,
      final Long paycomTime,
      final String password) {

    log.debug(
        "Create transaction: account {}, amount {}, transactionId {}",
        account,
        amount,
        transactionId);

    Account accountData;
    try {
      accountData = objectMapper.convertValue(account, Account.class);
      if (accountData == null
          || accountData.companyId() == null
          || amount == null
          || paycomId == null) {
        throw new IllegalArgumentException("Wrong request data");
      }
    } catch (Exception e) {
      log.error("Validation error: {}", e.getMessage());
      return Mono.just(
          new ErrorResult(new Error(-31050, Message.WRONG_RESPONSE_DATA), transactionId));
    }

    return paymeRepository
        .findByPaycomId(paycomId)
        .flatMap(
            existingTransaction -> handleExistingTransaction(existingTransaction, transactionId))
        .switchIfEmpty(
            Mono.defer(
                () ->
                    createNewTransaction(
                        accountData, amount, paycomId, paycomTime, password, transactionId)));
  }

  private Mono<PaymeInterface> handleExistingTransaction(
      PaymeTransactionEntity transaction, Long transactionId) {
    if (!STATE_IN_PROGRESS.equals(transaction.getState())) {
      return Mono.just(
          new ErrorResult(new Error(-31008, Message.UNABLE_TO_COMPLETE_OPERATION), transactionId));
    }

    if (System.currentTimeMillis() - transaction.getPaycomTime() > time_expired) {
      return Mono.just(
          new ErrorResult(
              new Error(-31008, Message.UNABLE_TO_COMPLETE_OPERATION, "transaction"),
              transactionId));
    }

    return Mono.just(new ResultResponse(transactionId, buildResponseMap(transaction)));
  }

  private Mono<PaymeInterface> createNewTransaction(
      Account account,
      Integer amount,
      String paycomId,
      Long paycomTime,
      String password,
      Long transactionId) {
    return paymeSettingService
        .getCurrent()
        .flatMap(
            payment -> {
              if (!Objects.equals(payment.getSecret(), password)) {
                return Mono.just(
                    (PaymeInterface)
                        new ErrorResult(new Error(-32504, Message.WRONG_HEADERS), transactionId));
              }
              if (amount < 50000) {
                return Mono.just(
                    (PaymeInterface)
                        new ErrorResult(
                            new Error(-31001, Message.WRONG_AMOUNT, "amount"), transactionId));
              }

              return userServiceClient
                  .determineBillingTypeByUUID(account.companyId())
                  .map(
                      billingType ->
                          new PaymeTransactionEntity(
                              account.companyId(),
                              billingType,
                              amount,
                              paycomId,
                              paycomTime,
                              STATE_IN_PROGRESS,
                              Instant.now().toEpochMilli(),
                              0L,
                              0L))
                  .flatMap(paymeRepository::save)
                  .map(saved -> new ResultResponse(transactionId, buildResponseMap(saved)));
            })
        .switchIfEmpty(
            Mono.just(new ErrorResult(new Error(-32504, Message.WRONG_HEADERS), transactionId)));
  }

  private Map<String, Object> buildResponseMap(PaymeTransactionEntity entity) {
    Map<String, Object> map = new HashMap<>();
    map.put("create_time", entity.getCreateTime());
    map.put("transaction", entity.getPaycomId());
    map.put("state", entity.getState());
    return map;
  }

  public Mono<PaymeInterface> performTransaction(
      final String paycomId, final Long transactionId, final String password) {

    log.debug("Perform transaction: {}, transaction: {}", paycomId, transactionId);

    Mono<PaymeInterface> transactionNotFound =
        Mono.just(new ErrorResult(new Error(-31003, Message.TRANSACTION_NOT_FOUND), transactionId));

    Mono<PaymeInterface> wrongHeaders =
        Mono.just(new ErrorResult(new Error(-32504, Message.WRONG_HEADERS), transactionId));

    return paymeRepository
        .findByPaycomId(paycomId)
        .flatMap(
            transaction ->
                paymeSettingService
                    .getCurrent()
                    .flatMap(
                        payment -> {
                          if (!Objects.equals(payment.getSecret(), password)) {
                            log.error(
                                "Authorization header error | payment not found or secret mismatch");
                            return wrongHeaders;
                          }

                          int state = transaction.getState();
                          long now = System.currentTimeMillis();

                          if (STATE_IN_PROGRESS == state) {
                            if (now - transaction.getPaycomTime() > time_expired) {
                              transaction.setState(STATE_CANCELED);
                              transaction.setCancelTime(now);
                              transaction.setReason(TRANSACTION_TIMEOUT);

                              return paymeRepository
                                  .save(transaction)
                                  .thenReturn(
                                      new ErrorResult(
                                          new Error(-31008, Message.UNABLE_TO_COMPLETE_OPERATION),
                                          transactionId));
                            }
                            transaction.setState(STATE_DONE);
                            transaction.setPerformTime(now);

                            return balanceHelper
                                .getBalanceEntity(transaction.getUuid(), BalanceType.SUMMA)
                                .flatMap(
                                    balance -> {
                                      TransactionEntity txEntity = new TransactionEntity();
                                      if (transaction.getBillingType() == BillingType.B2B) {
                                        txEntity.setCompanyId(transaction.getUuid());
                                      } else {
                                        txEntity.setUserId(transaction.getUuid());
                                      }
                                      txEntity.setBillingType(transaction.getBillingType());
                                      txEntity.setType(TransactionType.DEPOSIT_PAYME);
                                      txEntity.setBalanceId(balance.getId());
                                      txEntity.setAmount((transaction.getAmount() / (double) 100));

                                      return transactionRepository
                                          .save(txEntity)
                                          .flatMap(
                                              transactionEntity -> {
                                                balance.setBalance(
                                                    balance.getBalance()
                                                        + (transaction.getAmount() / (double) 100));
                                                return balanceRepository.save(balance);
                                              })
                                          .delayUntil(__b -> paymentRecorder.record(transaction.getUuid(), transaction.getAmount() / (double) 100, uz.hesap.service.common.util.enums.PaymentMethod.PAYME))
                                          // To'g'ridan-to'g'ri paket xaridi: mos PENDING
                                          // order bo'lsa balansdan paket grant qilinadi (net nol).
                                          .delayUntil(__s -> packageOrderService.settleIfPending(transaction.getUuid(), transaction.getAmount() / (double) 100))
                                          .then(paymeRepository.save(transaction))
                                          .map(
                                              savedTx -> {
                                                Map<String, Object> body = new HashMap<>();
                                                body.put("transaction", savedTx.getPaycomId());
                                                body.put("perform_time", savedTx.getPerformTime());
                                                body.put("state", STATE_DONE);

                                                log.info("Transaction with state done: {}", body);

                                                return (PaymeInterface)
                                                    new ResultResponse(transactionId, body);
                                              });
                                    });
                          }

                          if (STATE_DONE == state) {
                            Map<String, Object> body = new HashMap<>();
                            body.put("transaction", transaction.getPaycomId());
                            body.put("perform_time", transaction.getPerformTime());
                            body.put("state", STATE_DONE);

                            log.info("Transaction with state done: {}", body);

                            return Mono.just(
                                (PaymeInterface) new ResultResponse(transactionId, body));
                          }

                          // Payme yopilmagan tranzaksiyani qayta-qayta so'raydi — DEBUG.
                          log.debug("Unable to complete operation, state={}", state);
                          return Mono.just(
                              (PaymeInterface)
                                  new ErrorResult(
                                      new Error(-31008, Message.UNABLE_TO_COMPLETE_OPERATION),
                                      transactionId));
                        })
                    .switchIfEmpty(wrongHeaders))
        .switchIfEmpty(transactionNotFound);
  }

  public Mono<PaymeInterface> cancelTransaction(
      String paycomId, Integer reason, Long transactionId, final String password) {
    log.debug(
        "Cancel transaction: paycomId {}, reason {}, transactionId {}",
        paycomId,
        reason,
        transactionId);
    HashMap<String, Object> transactions = new HashMap<>();
    return paymeRepository
        .findByPaycomId(paycomId)
        .flatMap(
            transaction ->
                paymeSettingService
                    .getCurrent()
                    .flatMap(
                        payment -> {
                          if (!Objects.equals(payment.getSecret(), password)) {
                            log.error(
                                "Authorization header error | payment not found or secret mismatch");
                            return Mono.just(
                                new ErrorResult(
                                    new Error(-32504, Message.WRONG_HEADERS), transactionId));
                          }

                          if (transaction.getState().equals(STATE_IN_PROGRESS)) {
                            return updatePayment(reason, transactions, transaction);
                          } else if (transaction.getState().equals(STATE_DONE)) {

                            log.error("Unable to cancel");
                            return Mono.just(
                                new ErrorResult(
                                    new Error(-31007, Message.UNABLE_TO_CANCEL_TRANSACTION),
                                    transactionId));

                          } else {
                            HashMap<String, Object> transactionMap = new HashMap<>();
                            transactionMap.put("transaction", transaction.getPaycomId());
                            transactionMap.put("cancel_time", transaction.getCancelTime());
                            transactionMap.put("state", transaction.getState());
                            log.info("Cancel transaction: {}", transactionMap);
                            return Mono.just(new ResultResponse(transactionMap));
                          }
                        })
                    .switchIfEmpty(
                        Mono.just(
                            new ErrorResult(
                                new Error(-32504, Message.WRONG_HEADERS), transactionId))))
        .switchIfEmpty(
            Mono.just(
                new ErrorResult(new Error(-31003, Message.TRANSACTION_NOT_FOUND), transactionId)));
  }

  private Mono<PaymeInterface> updatePayment(
      Integer reason, HashMap<String, Object> transactions, PaymeTransactionEntity paymeEntity) {

    paymeEntity.setState(STATE_CANCELED);
    paymeEntity.setReason(reason);
    paymeEntity.setCancelTime(System.currentTimeMillis());
    return paymeRepository
        .save(paymeEntity)
        .flatMap(
            entity -> {
              transactions.put("transaction", paymeEntity.getPaycomId());
              transactions.put("cancel_time", paymeEntity.getCancelTime());
              transactions.put("state", STATE_CANCELED);
              log.info("Update payment to state cancel: transactions{}", transactions);
              return Mono.just(new ResultResponse(transactions));
            });
  }

  public Mono<PaymeInterface> checkTransaction(String paycomId, Long transactionId) {

    return paymeRepository
        .findByPaycomId(paycomId)
        .map(
            entity -> {
              log.info("Transaction: {}", entity);
              return (PaymeInterface)
                  new Result(
                      new CheckTransactionResult(
                          entity.getCreateTime(),
                          entity.getPerformTime(),
                          entity.getCancelTime(),
                          entity.getPaycomId(),
                          entity.getState(),
                          entity.getReason()));
            })
        .switchIfEmpty(
            Mono.just(
                new ErrorResult(new Error(-31003, Message.TRANSACTION_NOT_FOUND), transactionId)));
  }

  public Mono<PaymeInterface> getStatement(Long from, Long to) {
    log.debug("Get statement from: {}   to {} ", from, to);
    return paymeRepository
        .findAllByPaycomTimeBetween(from, to)
        .map(PaymeMapper.INSTANCE::convertEntityToMap)
        .collectList()
        .map(
            list -> {
              PaymeTransactionResp2 resp2 = new PaymeTransactionResp2();
              PaymeTransactionResp resp = new PaymeTransactionResp();

              resp.setTransactions(list);
              resp2.setResult(resp);
              return resp2;
            });
  }

  public Mono<CheckoutLinkModel> generateCheckoutLink(final UUID companyId, final Integer amount) {
    log.debug("CheckoutLinkModel: companyId: {} ", companyId);
    return paymeSettingService
        .getCurrent()
        .map(
            payment -> {
              String params =
                  Base64.getEncoder()
                      .encodeToString(
                          // Kassadagi rekvizit nomi `id` — link ham shu nom bilan.
                          ("m="
                                  + payment.getMerchantId()
                                  + ";ac.id="
                                  + companyId
                                  + ";a="
                                  + amount * 100)
                              .getBytes(StandardCharsets.UTF_8));
              CheckoutLinkModel linkModel =
                  new CheckoutLinkModel("https://checkout.paycom.uz/" + params);
              log.info("CheckoutLinkModel: {}", linkModel);
              return linkModel;
            })
        .switchIfEmpty(Mono.error(new InvalidArgumentException("settings not found")))
        .doOnNext(
            link ->
                sendPaymeLog(
                    null,
                    companyId,
                    "CHECKOUT_LINK",
                    "SUCCESS",
                    null,
                    "amount=" + amount,
                    toJson(link)))
        .doOnError(
            error ->
                sendPaymeLog(
                    null,
                    companyId,
                    "CHECKOUT_LINK",
                    "ERROR",
                    error.getMessage(),
                    "amount=" + amount,
                    null));
  }

  // Bir xil xato (method + tranzaksiya + xabar) LOG_DEDUP_WINDOW_MS ichida qayta
  // kelsa true qaytaradi — log yozilmaydi. Payme javobi bunga bog'liq emas.
  private boolean isDuplicateError(
      final String type, final String txId, final String errorMessage) {
    if (txId == null) return false;
    String key = type + "|" + txId + "|" + errorMessage;
    long now = System.currentTimeMillis();
    Long previous = lastErrorLoggedAt.get(key);
    if (previous != null && now - previous < LOG_DEDUP_WINDOW_MS) return true;
    // Cheksiz o'smasligi uchun eskirgan yozuvlarni tozalaymiz.
    if (lastErrorLoggedAt.size() > 1000) {
      lastErrorLoggedAt.entrySet().removeIf(e -> now - e.getValue() > LOG_DEDUP_WINDOW_MS);
    }
    lastErrorLoggedAt.put(key, now);
    return false;
  }

  // Payme so'rovi logini log-servisga (RabbitMQ) yuboradi. Log yozish asosiy
  // to'lov oqimini to'xtatmasligi kerak — xato bo'lsa yutiladi.
  private void sendPaymeLog(
      final UUID userId,
      final UUID companyId,
      final String type,
      final String status,
      final String errorMessage,
      final String request,
      final String response) {
    jmsPublisher
        .publish(
            new PaymeLogReply(
                userId, companyId, type, status, errorMessage, request, response, Instant.now()))
        .subscribe();
  }

  // Obyektni JSON string'ga aylantiradi; xato bo'lsa null (log to'xtab qolmasin).
  private String toJson(final Object value) {
    if (value == null) return null;
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      log.warn("Payme log JSON serialization failed: {}", e.getMessage());
      return null;
    }
  }
}
