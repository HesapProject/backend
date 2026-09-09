package uz.hesap.service.main.util;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import uz.hesap.service.common.exception.JsonParsingException;
import uz.hesap.service.common.exception.handler.ErrorCode;

public final class Utils {

  /**
   * Updates the given `valueToUpdate` instance with `overrides` object.
   *
   * @param valueToUpdate - an instance which is going to be updated
   * @param overrides - an object whose values will be overridden
   * @param mapper - object mapper instance
   * @param <T> - type of `valueToUpdate`
   * @return updated instance
   */
  public static <T> T updateValue(T valueToUpdate, Object overrides, final ObjectMapper mapper) {
    try {
      return mapper.updateValue(valueToUpdate, overrides);
    } catch (final JsonMappingException e) {
      throw new JsonParsingException(ErrorCode.JSON_PARSING_ERROR_CODE, e.getMessage());
    }
  }

  public static String toSnakeCase(String input) {
    if (input == null || input.isEmpty()) {
      return input;
    }
    String result =
        input.replaceAll("([a-z])([A-Z])", "$1_$2").replaceAll("([A-Z])([A-Z][a-z])", "$1_$2");
    return result.toLowerCase();
  }
}
