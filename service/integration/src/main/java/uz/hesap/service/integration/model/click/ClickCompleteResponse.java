package uz.hesap.service.integration.model.click;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClickCompleteResponse {
  @JsonProperty("click_trans_id")
  String clickTransId;

  @JsonProperty("merchant_trans_id")
  String merchantTransId;

  @JsonProperty("merchant_confirm_id")
  String merchantConfirmId;

  Integer error;

  @JsonProperty("error_note")
  String errorNote;

  @JsonProperty("service_id")
  Long serviceId;

  @JsonProperty("payment_id")
  Long paymentId;

  @JsonProperty("fiscal_items")
  List<ClickItemModel> items;

  @JsonProperty("received_ecash")
  Long receivedEcash;

  @JsonProperty("received_cash")
  Long receivedCash;

  @JsonProperty("received_card")
  Long receivedCard;
}
