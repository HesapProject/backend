package uz.hesap.service.integration.model.payme;

import lombok.Getter;
import lombok.Setter;
import uz.hesap.service.integration.util.PaymeInterface;

@Getter
@Setter
public class PaymeTransactionResp2 implements PaymeInterface {
  PaymeTransactionResp result;
}
