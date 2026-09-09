package uz.hesap.service.document.util;

/**
 * Sonni o'zbekcha (lotin) so'z bilan yozadi. Masalan: 10000000 → "o'n million", 100000 → "yuz
 * ming", 1234 → "bir ming ikki yuz o'ttiz to'rt". Faqat butun qism (manfiy bo'lmagan).
 */
public final class NumberToWordsUz {

  private static final String[] ONES = {
    "", "bir", "ikki", "uch", "to'rt", "besh", "olti", "yetti", "sakkiz", "to'qqiz"
  };
  private static final String[] TENS = {
    "", "o'n", "yigirma", "o'ttiz", "qirq", "ellik", "oltmish", "yetmish", "sakson", "to'qson"
  };
  // 10^3, 10^6, 10^9, 10^12 uchun ko'lam so'zlari (0-guruh = birliklar).
  private static final String[] SCALES = {"", "ming", "million", "milliard", "trillion"};

  private NumberToWordsUz() {}

  public static String toWords(long number) {
    if (number == 0) {
      return "nol";
    }
    if (number < 0) {
      return "minus " + toWords(-number);
    }

    StringBuilder sb = new StringBuilder();
    int scaleIndex = 0;
    long n = number;

    // Guruhlarni eng past 3 xonadan boshlab yig'amiz, keyin teskari joylaymiz.
    String[] groupWords = new String[SCALES.length];
    int count = 0;

    while (n > 0 && scaleIndex < SCALES.length) {
      int group = (int) (n % 1000);
      if (group > 0) {
        String words = threeDigits(group);
        if (scaleIndex == 1 && group == 1) {
          // 1000 = "ming" ("bir ming" emas).
          words = "";
        }
        String scale = SCALES[scaleIndex];
        String combined =
            (words.isEmpty() ? "" : words) + (scale.isEmpty() ? "" : (words.isEmpty() ? "" : " ") + scale);
        groupWords[count++] = combined.isEmpty() ? scale : combined;
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

  // 1..999 oralig'ini so'z bilan: "ikki yuz o'ttiz to'rt".
  private static String threeDigits(int n) {
    StringBuilder sb = new StringBuilder();
    int h = n / 100;
    int t = (n % 100) / 10;
    int o = n % 10;

    if (h > 0) {
      // 100 = "yuz" ("bir yuz" emas), 200 = "ikki yuz".
      if (h > 1) {
        sb.append(ONES[h]).append(' ');
      }
      sb.append("yuz");
    }
    if (t > 0) {
      if (sb.length() > 0) sb.append(' ');
      sb.append(TENS[t]);
    }
    if (o > 0) {
      if (sb.length() > 0) sb.append(' ');
      sb.append(ONES[o]);
    }
    return sb.toString();
  }
}
