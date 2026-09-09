package uz.hesap.service.document.util;

/**
 * Пишет число прописью по-русски. Напр.: 10000000 → "десять миллионов", 100000 → "сто тысяч",
 * 1234 → "одна тысяча двести тридцать четыре". Только неотрицательная целая часть.
 *
 * <p>Учитывает род (тысяча — женский: "одна тысяча", "две тысячи") и склонение масштабных слов
 * (тысяча/тысячи/тысяч, миллион/миллиона/миллионов).
 */
public final class NumberToWordsRu {

  private static final String[] ONES_M = {
    "", "один", "два", "три", "четыре", "пять", "шесть", "семь", "восемь", "девять",
    "десять", "одиннадцать", "двенадцать", "тринадцать", "четырнадцать", "пятнадцать",
    "шестнадцать", "семнадцать", "восемнадцать", "девятнадцать"
  };
  // Женский род отличается только для 1 и 2 (одна, две).
  private static final String[] ONES_F = {
    "", "одна", "две", "три", "четыре", "пять", "шесть", "семь", "восемь", "девять",
    "десять", "одиннадцать", "двенадцать", "тринадцать", "четырнадцать", "пятнадцать",
    "шестнадцать", "семнадцать", "восемнадцать", "девятнадцать"
  };
  private static final String[] TENS = {
    "", "", "двадцать", "тридцать", "сорок", "пятьдесят", "шестьдесят", "семьдесят",
    "восемьдесят", "девяносто"
  };
  private static final String[] HUNDREDS = {
    "", "сто", "двести", "триста", "четыреста", "пятьсот", "шестьсот", "семьсот",
    "восемьсот", "девятьсот"
  };
  // Масштабные слова: 3 формы склонения [1 / 2-4 / 5-0], для 10^3, 10^6, 10^9, 10^12.
  private static final String[][] SCALES = {
    {"", "", ""},
    {"тысяча", "тысячи", "тысяч"},
    {"миллион", "миллиона", "миллионов"},
    {"миллиард", "миллиарда", "миллиардов"},
    {"триллион", "триллиона", "триллионов"}
  };

  private NumberToWordsRu() {}

  public static String toWords(long number) {
    if (number == 0) {
      return "ноль";
    }
    if (number < 0) {
      return "минус " + toWords(-number);
    }

    StringBuilder sb = new StringBuilder();
    int scaleIndex = 0;
    long n = number;

    // Собираем группы с младших 3 цифр, затем выводим в обратном порядке.
    String[] groupWords = new String[SCALES.length];
    int count = 0;

    while (n > 0 && scaleIndex < SCALES.length) {
      int group = (int) (n % 1000);
      if (group > 0) {
        // Тысячи — женский род ("одна тысяча"), остальные — мужской.
        boolean feminine = scaleIndex == 1;
        String words = threeDigits(group, feminine);
        String scale = SCALES[scaleIndex][pluralForm(group)];
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

  // Выбор формы склонения масштабного слова по значению группы (1..999).
  private static int pluralForm(int n) {
    int mod100 = n % 100;
    if (mod100 >= 11 && mod100 <= 14) {
      return 2; // тысяч, миллионов
    }
    int mod10 = n % 10;
    if (mod10 == 1) {
      return 0; // тысяча, миллион
    }
    if (mod10 >= 2 && mod10 <= 4) {
      return 1; // тысячи, миллиона
    }
    return 2;
  }

  // 1..999 прописью: "двести тридцать четыре". feminine — для группы тысяч (одна/две).
  private static String threeDigits(int n, boolean feminine) {
    StringBuilder sb = new StringBuilder();
    int h = n / 100;
    int rem = n % 100;
    String[] ones = feminine ? ONES_F : ONES_M;

    if (h > 0) {
      sb.append(HUNDREDS[h]);
    }
    if (rem > 0) {
      if (sb.length() > 0) sb.append(' ');
      if (rem < 20) {
        sb.append(ones[rem]);
      } else {
        sb.append(TENS[rem / 10]);
        if (rem % 10 > 0) {
          sb.append(' ').append(ones[rem % 10]);
        }
      }
    }
    return sb.toString();
  }
}
