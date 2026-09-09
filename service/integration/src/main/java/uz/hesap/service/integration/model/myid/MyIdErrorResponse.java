package uz.hesap.service.integration.model.myid;

import java.util.List;
import java.util.Map;

public record MyIdErrorResponse(Object detail) {
  public String getMessage() {
    return switch (detail) {
      case null -> null;
      case String s -> s;
      case List<?> list when !list.isEmpty() -> {
        if (list.getFirst() instanceof Map<?, ?> map && map.get("msg") != null) {
          String msg = map.get("msg").toString();
          // Pydantic xato: loc = ["body", "<field>"] — qaysi maydon ekanini ko'rsatamiz.
          String field = "";
          if (map.get("loc") instanceof List<?> loc && !loc.isEmpty()) {
            field = loc.get(loc.size() - 1).toString();
          }
          yield field.isEmpty() ? msg : (field + ": " + msg);
        }
        yield detail.toString();
      }
      default -> detail.toString();
    };
  }
}
