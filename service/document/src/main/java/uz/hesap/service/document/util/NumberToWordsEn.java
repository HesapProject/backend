package uz.hesap.service.document.util;

/**
 * Writes a number in English words. E.g. 10000000 → "ten million", 100000 → "one hundred
 * thousand", 1234 → "one thousand two hundred thirty four". Non-negative integer part only.
 */
public final class NumberToWordsEn {

  private static final String[] ONES = {
    "", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine",
    "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen",
    "seventeen", "eighteen", "nineteen"
  };
  private static final String[] TENS = {
    "", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety"
  };
  // 10^3, 10^6, 10^9, 10^12 scale words (group 0 = units).
  private static final String[] SCALES = {"", "thousand", "million", "billion", "trillion"};

  private NumberToWordsEn() {}

  public static String toWords(long number) {
    if (number == 0) {
      return "zero";
    }
    if (number < 0) {
      return "minus " + toWords(-number);
    }

    StringBuilder sb = new StringBuilder();
    int scaleIndex = 0;
    long n = number;

    // Collect groups from the lowest 3 digits up, then emit in reverse.
    String[] groupWords = new String[SCALES.length];
    int count = 0;

    while (n > 0 && scaleIndex < SCALES.length) {
      int group = (int) (n % 1000);
      if (group > 0) {
        String words = threeDigits(group);
        String scale = SCALES[scaleIndex];
        groupWords[count++] = scale.isEmpty() ? words : words + " " + scale;
      }
      n /= 1000;
      scaleIndex++;
    }

    for (int i = count - 1; i >= 0; i--) {
      if (sb.length() > 0) {
        sb.append(' ');
      }
      sb.append(groupWords[i]);
    }
    return sb.toString().trim();
  }

  // 1..999 as words: "two hundred thirty four".
  private static String threeDigits(int n) {
    StringBuilder sb = new StringBuilder();
    int h = n / 100;
    int rem = n % 100;

    if (h > 0) {
      sb.append(ONES[h]).append(" hundred");
    }
    if (rem > 0) {
      if (sb.length() > 0) sb.append(' ');
      if (rem < 20) {
        sb.append(ONES[rem]);
      } else {
        sb.append(TENS[rem / 10]);
        if (rem % 10 > 0) {
          sb.append(' ').append(ONES[rem % 10]);
        }
      }
    }
    return sb.toString();
  }
}
