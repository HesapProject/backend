package uz.hesap.service.main.model.eskiz;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EskizMessageRequest(
    @JsonProperty("mobile_phone") String phone, String message, String from) {

  public EskizMessageRequest(String phone, String message, String from) {
    this.phone = phone.replace("+", "");
    this.message = message;
    this.from = from;
  }
}
