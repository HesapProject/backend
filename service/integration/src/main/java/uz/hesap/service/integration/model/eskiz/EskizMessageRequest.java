package uz.hesap.service.integration.model.eskiz;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Eskiz SMS message request */
public record EskizMessageRequest(
    @JsonProperty("mobile_phone") String phone, String message, String from) {
  public EskizMessageRequest(String phone, String message, String from) {
    // Remove + from phone number as Eskiz expects it without +
    this.phone = phone != null ? phone.replace("+", "") : null;
    this.message = message;
    this.from = from;
  }
}
