package uz.hesap.service.integration.model.payme;

import java.util.Map;
import uz.hesap.service.integration.util.PaymeInterface;

public record ResultResponse(Long id, Map<String, ?> result) implements PaymeInterface {

  public ResultResponse(Map<String, ?> result) {
    this(null, result);
  }

  //  public ResultResponse(Long id, Map<String, ?> result) {
  //    this.id = id;
  //    this.result = result;
  //  }
}
