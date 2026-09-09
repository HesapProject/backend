package uz.hesap.service.document.util;

/**
 * Sonni tanlangan tilda so'z bilan yozadi (uz/ru/en). Til bo'sh yoki noma'lum bo'lsa — o'zbekcha.
 */
public final class NumberToWords {

  private NumberToWords() {}

  public static String toWords(long number, String lang) {
    if (lang == null) {
      return NumberToWordsUz.toWords(number);
    }
    return switch (lang.toLowerCase()) {
      case "ru" -> NumberToWordsRu.toWords(number);
      case "en" -> NumberToWordsEn.toWords(number);
      default -> NumberToWordsUz.toWords(number);
    };
  }
}
