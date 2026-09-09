package uz.hesap.service.integration.service.payment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.message.ClickLogReply;
import uz.hesap.service.integration.domain.*;
import uz.hesap.service.integration.domain.enums.BalanceType;
import uz.hesap.service.integration.domain.enums.BillingType;
import uz.hesap.service.integration.domain.enums.TransactionType;
import uz.hesap.service.integration.model.click.ClickErrors;
import uz.hesap.service.integration.model.click.ClickResponse;
import uz.hesap.service.integration.model.payme.CheckoutLinkModel;
import uz.hesap.service.integration.domain.ClickSettingEntity;
import uz.hesap.service.integration.repository.BalanceRepository;
import uz.hesap.service.integration.repository.ClickRepository;
import uz.hesap.service.integration.repository.TransactionRepository;
import uz.hesap.service.integration.service.ClickSettingService;
import uz.hesap.service.integration.service.client.UserServiceClient;
import uz.hesap.service.jms.JmsPublisher;

@Log4j2
@Service
public class ClickService {
  private final ClickRepository clickRepository;
  private final ClickSettingService clickSettingService;
  private final BalanceHelper balanceHelper;
  private final TransactionRepository transactionRepository;
  private final BalanceRepository balanceRepository;
  private final UserServiceClient userServiceClient;
  private final ObjectMapper objectMapper;
  private final JmsPublisher jmsPublisher;
  private final PaymentRecorder paymentRecorder;
  private final PackageOrderService packageOrderService;

  public ClickService(
      final ClickRepository clickRepository,
      final ClickSettingService clickSettingService,
      final BalanceHelper balanceHelper,
      final TransactionRepository transactionRepository,
      final BalanceRepository balanceRepository,
      final UserServiceClient userServiceClient,
      final ObjectMapper objectMapper,
      final JmsPublisher jmsPublisher,
      final PaymentRecorder paymentRecorder,
      final PackageOrderService packageOrderService) {
    this.clickRepository = clickRepository;
    this.clickSettingService = clickSettingService;
    this.balanceHelper = balanceHelper;
    this.transactionRepository = transactionRepository;
    this.balanceRepository = balanceRepository;
    this.userServiceClient = userServiceClient;
    this.objectMapper = objectMapper;
    this.jmsPublisher = jmsPublisher;
    this.paymentRecorder = paymentRecorder;
    this.packageOrderService = packageOrderService;
  }

  public Mono<ClickResponse> prepare(ClickResponse clickResponse) {
    log.debug("prepare: clickResponse: [{}]", clickResponse);
    UUID uuid;
    try {
      uuid = UUID.fromString(clickResponse.getMerchant_trans_id());
    } catch (Exception e) {
      log.error("Cannot cast order id to uuid {} ", clickResponse.getMerchant_trans_id());
      return Mono.just(createErrorPrepare(ClickErrors.ORDER_NOT_FOUND, clickResponse));
    }

    return clickSettingService
        .getCurrent()
        .flatMap(
            paymentSetting -> {
              if (!verifyMD5Hash(paymentSetting, clickResponse, Boolean.TRUE)) {
                ClickResponse response = new ClickResponse();
                response.setError(ClickErrors.SIGN_CHECK_FAILED.getError());
                response.setClick_trans_id(clickResponse.getClick_trans_id());
                response.setMerchant_trans_id(clickResponse.getMerchant_trans_id());
                response.setError_note(ClickErrors.SIGN_CHECK_FAILED.getErrorNote());
                log.info("Prepare: response: {}", response);
                return Mono.just(response);
              }

              if (clickResponse.getAmount() < 500) {
                log.error("Order price not equal");
                return Mono.just(
                    createErrorPrepare(ClickErrors.INCORRECT_PARAMETER_AMOUNT, clickResponse));
              }

              return saveTransactionPrepare(clickResponse, uuid)
                  .map(
                      savedTransactionResponse -> {
                        ClickResponse response = new ClickResponse();
                        response.setError(ClickErrors.SUCCESS.getError());
                        response.setClick_trans_id(clickResponse.getClick_trans_id());
                        response.setMerchant_trans_id(clickResponse.getMerchant_trans_id());
                        response.setMerchant_prepare_id(savedTransactionResponse.toString());
                        response.setError_note(ClickErrors.SUCCESS.getErrorNote());
                        response.setParam2("1");
                        return response;
                      });
            })
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  ClickResponse response = new ClickResponse();
                  response.setError(ClickErrors.USER_NOT_FOUND.getError());
                  response.setClick_trans_id(clickResponse.getClick_trans_id());
                  response.setMerchant_trans_id(clickResponse.getMerchant_trans_id());
                  response.setError_note(ClickErrors.USER_NOT_FOUND.getErrorNote());
                  return Mono.just(response);
                }))
        .doOnNext(resp -> logClick("PREPARE", uuid, clickResponse, resp))
        .doOnError(
            error ->
                sendClickLog(
                    null,
                    uuid,
                    "PREPARE",
                    "ERROR",
                    error.getMessage(),
                    toJson(clickResponse),
                    null));
  }

  public Mono<ClickResponse> complete(ClickResponse clickResponse) {
    log.debug("prepare: clickResponse: {}", clickResponse);
    final UUID orderId = parseUuid(clickResponse.getMerchant_trans_id());
    if (clickResponse.getError() == -5017) {

      return cancelTransaction(clickResponse)
          .map(
              entity -> {
                ClickResponse response = new ClickResponse();
                response.setError(ClickErrors.CANCELLED.getError());
                response.setClick_trans_id(clickResponse.getClick_trans_id());
                response.setMerchant_trans_id(clickResponse.getMerchant_trans_id());
                response.setMerchant_confirm_id(clickResponse.getMerchant_prepare_id());
                response.setError_note(ClickErrors.CANCELLED.getErrorNote());
                log.info("Complete: response: {}", response);
                return response;
              })
          .doOnNext(resp -> logClick("COMPLETE", orderId, clickResponse, resp));
    }
    return clickSettingService
        .getCurrent()
        .flatMap(
            paymentSetting -> {
              if (!verifyMD5Hash(paymentSetting, clickResponse, Boolean.FALSE)) {
                ClickResponse response = new ClickResponse();
                response.setError(ClickErrors.SIGN_CHECK_FAILED.getError());
                response.setClick_trans_id(clickResponse.getClick_trans_id());
                response.setMerchant_trans_id(clickResponse.getMerchant_trans_id());
                response.setMerchant_confirm_id(clickResponse.getMerchant_prepare_id());
                response.setError_note(ClickErrors.SIGN_CHECK_FAILED.getErrorNote());
                log.info("Complete: sig check failed response: {}", response);
                return Mono.just(response);
              }

              if (clickResponse.getAmount() < 500) {
                log.error("Order price not equal amount [{}]", clickResponse.getAmount());
                return Mono.just(
                    createErrorPrepare(ClickErrors.INCORRECT_PARAMETER_AMOUNT, clickResponse));
              }

              return getTransaction(clickResponse.getClick_trans_id())
                  .flatMap(
                      transaction -> {
                        if (!Objects.equals(
                            transaction.getId().toString(),
                            clickResponse.getMerchant_prepare_id())) {
                          ClickResponse response = new ClickResponse();
                          response.setError(ClickErrors.TRANSACTION_NOT_FOUND.getError());
                          response.setClick_trans_id(clickResponse.getClick_trans_id());
                          response.setMerchant_trans_id(clickResponse.getMerchant_trans_id());
                          response.setError_note(ClickErrors.TRANSACTION_NOT_FOUND.getErrorNote());
                          return Mono.just(response);
                        }

                        if (transaction.getError() != 0) {
                          return Mono.just(
                              createErrorComplete(ClickErrors.CANCELLED, clickResponse));
                        }

                        UUID uniqueId = transaction.getUuid();

                        BillingType billingType = transaction.getBillingType();

                        return balanceHelper
                            .getBalanceEntity(uniqueId, BalanceType.SUMMA, billingType)
                            .flatMap(
                                balance -> {
                                  TransactionEntity txEntity = new TransactionEntity();
                                  txEntity.setBillingType(transaction.getBillingType());
                                  if (billingType == BillingType.B2B) {
                                    txEntity.setCompanyId(transaction.getUuid());
                                  } else {
                                    txEntity.setUserId(transaction.getUuid());
                                  }
                                  txEntity.setType(TransactionType.DEPOSIT_CLICK);
                                  txEntity.setAmount(Double.valueOf(clickResponse.getAmount()));
                                  txEntity.setBalanceId(balance.getId());

                                  return transactionRepository
                                      .save(txEntity)
                                      .flatMap(
                                          transactionEntity -> {
                                            balance.setBalance(
                                                balance.getBalance() + clickResponse.getAmount());
                                            return paymentRecorder.record(uniqueId, Double.valueOf(clickResponse.getAmount()), uz.hesap.service.common.util.enums.PaymentMethod.CLICK).then(balanceRepository.save(balance));
                                          })
                                      // To'g'ridan-to'g'ri paket xaridi: mos PENDING order
                                      // bo'lsa balansdan paket grant qilinadi (net nol).
                                      .delayUntil(
                                          __s ->
                                              packageOrderService.settleIfPending(
                                                  uniqueId,
                                                  Double.valueOf(clickResponse.getAmount())))
                                      .flatMap(
                                          b ->
                                              saveTransactionComplete(
                                                      clickResponse, transaction.getId())
                                                  .map(
                                                      saveTransactionResponse -> {
                                                        ClickResponse response =
                                                            new ClickResponse();
                                                        response.setError(
                                                            ClickErrors.SUCCESS.getError());
                                                        response.setClick_trans_id(
                                                            clickResponse.getClick_trans_id());
                                                        response.setMerchant_trans_id(
                                                            clickResponse.getMerchant_trans_id());
                                                        response.setMerchant_confirm_id(
                                                            transaction.getId().toString());
                                                        response.setError_note(
                                                            ClickErrors.SUCCESS.getErrorNote());

                                                        return response;
                                                      })
                                                  .switchIfEmpty(
                                                      Mono.defer(
                                                          () -> {
                                                            ClickResponse response =
                                                                new ClickResponse();
                                                            response.setError(
                                                                ClickErrors.TRANSACTION_NOT_FOUND
                                                                    .getError());
                                                            response.setClick_trans_id(
                                                                clickResponse.getClick_trans_id());
                                                            response.setMerchant_trans_id(
                                                                clickResponse
                                                                    .getMerchant_trans_id());
                                                            response.setMerchant_confirm_id(
                                                                transaction.getId().toString());
                                                            response.setError_note(
                                                                ClickErrors.TRANSACTION_NOT_FOUND
                                                                    .getErrorNote());
                                                            return Mono.just(response);
                                                          })));
                                });
                      })
                  .switchIfEmpty(
                      Mono.defer(
                          () -> {
                            ClickResponse response = new ClickResponse();
                            response.setError(ClickErrors.TRANSACTION_NOT_FOUND.getError());
                            response.setClick_trans_id(clickResponse.getClick_trans_id());
                            response.setMerchant_trans_id(clickResponse.getMerchant_trans_id());
                            response.setError_note(
                                ClickErrors.TRANSACTION_NOT_FOUND.getErrorNote());
                            return Mono.just(response);
                          }));
            })
        .doOnNext(resp -> logClick("COMPLETE", orderId, clickResponse, resp))
        .doOnError(
            error ->
                sendClickLog(
                    null,
                    orderId,
                    "COMPLETE",
                    "ERROR",
                    error.getMessage(),
                    toJson(clickResponse),
                    null));
  }

  public Boolean verifyMD5Hash(
      ClickSettingEntity clickPayment, ClickResponse clickResponse, Boolean prepare) {
    log.debug("Verify MD5 Hash: clickResponse: {}", clickResponse);
    log.debug("Verify MD5 Hash: prepare: {}", prepare);

    String merchantPrepId = prepare ? "" : clickResponse.getMerchant_prepare_id();
    String input =
        clickResponse.getClick_trans_id()
            + clickResponse.getService_id()
            + clickPayment.getSecretKey()
            + clickResponse.getMerchant_trans_id()
            + merchantPrepId
            + clickResponse.getAmount()
            + clickResponse.getAction()
            + clickResponse.getSign_time();
    log.info("Parameters: {}", input);
    String md5Hash = DigestUtils.md5Hex(input.getBytes(StandardCharsets.UTF_8));
    log.info("MD5 Hash: {}", md5Hash);
    if (!md5Hash.equals(clickResponse.getSign_string())) {
      return Boolean.FALSE;
    } else {
      return Boolean.TRUE;
    }
  }

  private Mono<ClickEntity> getTransaction(String clickTransId) {
    log.debug("Get Transaction: clickTransactionId: {}", clickTransId);
    return clickRepository.findByClickTransId(clickTransId);
  }

  private static String getParam(final Integer amount, final ClickSettingEntity payment, UUID uuid) {

    return "service_id="
        + payment.getServiceId()
        + "&merchant_id="
        + payment.getMerchantId()
        + "&amount="
        + amount
        + "&transaction_param="
        + uuid;
  }

  private Mono<ClickEntity> cancelTransaction(ClickResponse clickResponse) {
    return clickRepository
        .findByClickTransIdAndMerchantTransId(
            clickResponse.getClick_trans_id(), clickResponse.getMerchant_trans_id())
        .flatMap(
            entity -> {
              entity.setError(ClickErrors.CANCELLED.getError());
              entity.setErrorNote(ClickErrors.CANCELLED.getErrorNote());
              entity.setSignTime(Instant.now());
              log.info("Transaction canceled entity: {}", entity);
              return clickRepository.save(entity);
            })
        .switchIfEmpty(Mono.empty());
  }

  private Mono<UUID> saveTransactionPrepare(final ClickResponse clickResponse, final UUID uuid) {
    log.debug("Save transaction prepare: clickResponse = {}", clickResponse);
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    LocalDateTime localDateTime = LocalDateTime.parse(clickResponse.getSign_time(), formatter);
    Instant instant = localDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant();

    return getClickEntity(clickResponse, uuid, instant)
        .flatMap(entity -> clickRepository.save(entity).map(ClickEntity::getId));
  }

  private Mono<ClickEntity> getClickEntity(
      final ClickResponse clickResponse, final UUID transactionId, final Instant instant) {
    return userServiceClient
        .determineBillingTypeByUUID(transactionId)
        .map(
            billingType -> {
              ClickEntity clickEntity = new ClickEntity();
              clickEntity.setUuid(transactionId);
              clickEntity.setBillingType(billingType);
              clickEntity.setClickTransId(clickResponse.getClick_trans_id());
              clickEntity.setServiceId(clickResponse.getService_id());
              clickEntity.setClickPaydocId(clickResponse.getClick_paydoc_id());
              clickEntity.setAmount(clickResponse.getAmount());
              clickEntity.setAction(clickResponse.getAction());
              clickEntity.setError(clickResponse.getError());
              clickEntity.setMerchantTransId(clickResponse.getMerchant_trans_id());
              clickEntity.setErrorNote(clickResponse.getError_note());
              clickEntity.setSignTime(instant);
              clickEntity.setSignString(clickResponse.getSign_string());
              return clickEntity;
            });
  }

  private Mono<ClickEntity> saveTransactionComplete(ClickResponse clickResponse, UUID confirmId) {
    log.debug("Saving transaction complete: clickResponse: {}", clickResponse);
    log.debug("Saving transaction complete: confirmId: {}", confirmId);
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    LocalDateTime localDateTime = LocalDateTime.parse(clickResponse.getSign_time(), formatter);
    Instant instant = localDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant();

    return clickRepository
        .findByClickTransIdAndId(clickResponse.getClick_trans_id(), confirmId)
        .flatMap(
            entity -> {
              entity.setAction(clickResponse.getAction());
              entity.setError(clickResponse.getError());
              entity.setErrorNote(clickResponse.getError_note());
              entity.setSignTime(instant);
              entity.setSignString(clickResponse.getSign_string());
              entity.setClickTransId(clickResponse.getClick_trans_id());
              entity.setMerchantTransId(clickResponse.getMerchant_trans_id());
              log.info("saveTransactionComplete: {}", entity);
              return clickRepository.save(entity);
            });
  }

  private ClickResponse createErrorPrepare(ClickErrors errorType, ClickResponse clickResponse) {
    log.debug("createErrorPrepare: errorType: {}", errorType);
    log.debug("createErrorPrepare: clickResponse: {}", clickResponse);
    ClickResponse response = new ClickResponse();
    response.setError(errorType.getError());
    response.setClick_trans_id(clickResponse.getClick_trans_id());
    response.setMerchant_trans_id(clickResponse.getMerchant_trans_id());
    response.setMerchant_prepare_id(clickResponse.getMerchant_prepare_id());
    response.setError_note(errorType.getErrorNote());
    log.info("createErrorPrepare: response {}", response);
    return response;
  }

  private ClickResponse createErrorComplete(ClickErrors errorType, ClickResponse clickResponse) {
    log.debug("createErrorComplete: errorType: {}, ClickErrors: {} ", errorType, clickResponse);
    ClickResponse response = new ClickResponse();
    response.setError(errorType.getError());
    response.setClick_trans_id(clickResponse.getClick_trans_id());
    response.setMerchant_trans_id(clickResponse.getMerchant_trans_id());
    response.setMerchant_confirm_id(clickResponse.getMerchant_prepare_id());
    response.setError_note(errorType.getErrorNote());
    log.info("createErrorComplete: response {}", response);
    return response;
  }

  public Mono<CheckoutLinkModel> generateCheckoutLink(final UUID uuid, final Integer amount) {
    log.debug("Click checkout url company  or user [{}] ,amount [{}]", uuid, amount);
    return clickSettingService
        .getCurrent()
        .map(
            payment -> {
              String params = getParam(amount, payment, uuid);

              log.info("click checkout url: {}", "https://my.click.uz/services/pay/?" + params);
              return new CheckoutLinkModel("https://my.click.uz/services/pay/?" + params);
            })
        .doOnNext(
            link ->
                sendClickLog(
                    null,
                    uuid,
                    "CHECKOUT_LINK",
                    "SUCCESS",
                    null,
                    "amount=" + amount,
                    toJson(link)))
        .doOnError(
            error ->
                sendClickLog(
                    null,
                    uuid,
                    "CHECKOUT_LINK",
                    "ERROR",
                    error.getMessage(),
                    "amount=" + amount,
                    null));
  }

  // prepare/complete javobini logga yozadi (Click error=0 bo'lsa muvaffaqiyat).
  private void logClick(
      final String type, final UUID orderId, final ClickResponse request, final ClickResponse resp) {
    boolean ok = resp != null && resp.getError() != null && resp.getError() == 0;
    sendClickLog(
        null,
        orderId,
        type,
        ok ? "SUCCESS" : "ERROR",
        ok ? null : (resp == null ? null : resp.getError_note()),
        toJson(request),
        toJson(resp));
  }

  // Click so'rovi logini log-servisga (RabbitMQ) yuboradi; xato bo'lsa yutiladi.
  private void sendClickLog(
      final UUID userId,
      final UUID companyId,
      final String type,
      final String status,
      final String errorMessage,
      final String request,
      final String response) {
    jmsPublisher
        .publish(
            new ClickLogReply(
                userId, companyId, type, status, errorMessage, request, response, Instant.now()))
        .subscribe();
  }

  // merchant_trans_id ni UUID'ga aylantiradi; xato bo'lsa null.
  private UUID parseUuid(final String value) {
    if (value == null) return null;
    try {
      return UUID.fromString(value);
    } catch (Exception e) {
      return null;
    }
  }

  // Obyektni JSON string'ga aylantiradi; xato bo'lsa null (log to'xtab qolmasin).
  private String toJson(final Object value) {
    if (value == null) return null;
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      log.warn("Click log JSON serialization failed: {}", e.getMessage());
      return null;
    }
  }
}
