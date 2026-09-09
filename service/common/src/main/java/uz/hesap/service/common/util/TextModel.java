package uz.hesap.service.common.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import uz.hesap.service.common.util.enums.Language;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TextModel(String uz, String ru, String en) implements Serializable {

  public String get(Language lang) {
    if (lang == Language.RU) {
      return ru();
    }
    if (lang == Language.EN) {
      return en();
    }
    return uz();
  }

  public TextModel truncate(int length) {
    return new TextModel(
        truncateStr(uz(), length), truncateStr(ru(), length), truncateStr(en(), length));
  }

  private String truncateStr(String str, int length) {
    if (str == null) return null;
    return str.length() > length ? str.substring(0, length) : str;
  }
}
