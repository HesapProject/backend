package uz.hesap.service.integration.model.click;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ClickPrepareResponse {
  Integer error;
  String error_note;
  String click_trans_id;
  String merchant_trans_id;
  String merchant_prepare_id;
}
