package uz.hesap.service.integration.model.click;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ClickLogResponse(
    UUID id, String method, Map<String, Object> parameters, Instant createdDate) {
  public static final String PREPARE_IN = "prepare_in";
  public static final String PREPARE_OUT = "prepare_out";
  public static final String COMPLETE_IN = "complete_in";
  public static final String COMPLETE_OUT = "complete_out";
}
