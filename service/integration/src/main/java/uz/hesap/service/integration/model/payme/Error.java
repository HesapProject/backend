package uz.hesap.service.integration.model.payme;

import uz.hesap.service.integration.util.PaymeInterface;

public record Error(Integer code, Message message, String data) implements PaymeInterface {
  public Error(Integer code, Message message) {
    this(code, message, null);
  }
}
