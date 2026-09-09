package uz.hesap.service.integration.model.click;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;
import lombok.ToString;

@ToString
public class ClickRequest {
  private UUID id;

  @JsonProperty("click_trans_id")
  private String click_trans_id;

  @JsonProperty("service_id")
  private Long service_id;

  @JsonProperty("click_paydoc_id")
  private Long click_paydoc_id;

  @JsonProperty("merchant_prepare_id")
  private String merchant_prepare_id;

  @JsonProperty("merchant_confirm_id")
  private String merchant_confirm_id;

  private Long amount;
  private Integer action;
  private Integer error;

  @JsonProperty("merchant_trans_id")
  private String merchant_trans_id;

  @JsonProperty("error_note")
  private String error_note;

  @JsonProperty("sign_string")
  private String sign_string;

  @JsonProperty("sign_time")
  private String sign_time;

  String param2;
}
