package uz.hesap.service.main.model;

import org.springframework.util.Assert;
import uz.hesap.service.common.util.TextModel;

public record FaqRequest(TextModel title, TextModel answer) {
  public FaqRequest {
    Assert.notNull(title != null && answer != null, "Answer and title required");
  }
}
