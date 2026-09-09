package uz.hesap.service.log.util;

public class Utils {

  public static String toSnakeCase(String input) {
    if (input == null || input.isEmpty()) {
      return input;
    }
    String result =
        input.replaceAll("([a-z])([A-Z])", "$1_$2").replaceAll("([A-Z])([A-Z][a-z])", "$1_$2");

    return result.toLowerCase();
  }
}
