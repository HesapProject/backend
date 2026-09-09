package uz.hesap.service.integration.model.payme;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import uz.hesap.service.integration.util.PaymeInterface;

@Getter
@Setter
public class PaymeTransactionResp implements PaymeInterface {
  List<PaymeTransactionModel> transactions;
}
