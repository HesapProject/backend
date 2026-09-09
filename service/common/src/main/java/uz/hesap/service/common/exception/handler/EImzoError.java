package uz.hesap.service.common.exception.handler;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum EImzoError {
  SUCCESS(1, "Успешно"),
  CHECK_CERT_STATUS_FAILED(
      -1, "Неудалось проверить статус сертификата. Посмотрите лог E-IMZO-SERVER."),
  SIGNING_TIME_INVALID(
      -5, "Время подписи недействительна. Проверьте дату и время компьютера пользователя."),
  SIGNATURE_INVALID(-10, "ЭЦП недействительна"),
  CERT_INVALID(-11, "Сертификат недействитеlen"),
  CERT_INVALID_AT_SIGNING_TIME(-12, "Сертификат недействителен на дату подписи"),
  CHALLENGE_NOT_FOUND(-20, "Не найден challenge или срок его истек. Повторите заного."),
  TIMESTAMP_STATUS_FAILED(
      -21, "Неудалось проверить статус сертификата Timestamp. Посмотрите лог E-IMZO-SERVER."),
  TIMESTAMP_HASH_INVALID(-22, "ЭЦП или хеш Timestamp недействительна"),
  TIMESTAMP_CERT_INVALID(-23, "Сертификат Timestamp недействителен"),
  TIMESTAMP_CERT_INVALID_AT_SIGNING_TIME(
      -24, "Сертификат Timestamp недействителен на дату подписи"),
  UNKNOWN_ERROR(999, "Noma'lum xatolik");

  private final int code;
  private final String message;

  private static final Map<Integer, EImzoError> CACHE =
      Arrays.stream(values()).collect(Collectors.toMap(e -> e.code, Function.identity()));

  EImzoError(int code, String message) {
    this.code = code;
    this.message = message;
  }

  public static String getMessageByCode(int code) {
    EImzoError status = CACHE.getOrDefault(code, UNKNOWN_ERROR);
    return status.message;
  }

  public int getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }

  public boolean isSuccess() {
    return this == SUCCESS;
  }
}
