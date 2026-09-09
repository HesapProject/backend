package uz.hesap.service.document.service.document.structure;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Matn ichidagi {@code {{...}}} placeholder'larni JRXML {@code textFieldExpression} ifodasiga
 * aylantiradi (string konkatenatsiya). DocumentGenerator render kontraktiga mos:
 *
 * <ul>
 *   <li>{@code {{number}}}, {@code {{price}}} ... → {@code $P{number}} (avto param)
 *   <li>{@code {{seller.fullName}}} → {@code String.valueOf(((java.util.Map)$P{seller}).get("fullName"))}
 *   <li>{@code {{field:customKey}}} → {@code $P{customKey}} (template_field STRING)
 * </ul>
 *
 * <p>Natija CDATA ichiga qo'yiladi — shu sababli literal qism {@code "}/{@code \n}/{@code ]]>}
 * dan xavfsiz escape qilinadi.
 */
public final class PlaceholderCompiler {

  private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([\\w.:]+)\\s*}}");

  // Bare token sifatida ruxsat etilgan avto-paramlar (DocumentGenerator beradi).
  private static final Set<String> AUTO_PARAMS =
      Set.of(
          "number",
          "price",
          "currency",
          "priceWords",
          "priceInWords",
          "priceText",
          "priceWord",
          "createdDate",
          "address");

  private PlaceholderCompiler() {}

  /**
   * Matndagi placeholder'lardan param nomlarini ajratadi:
   *
   * <ul>
   *   <li>{@code scalar} — bare tokenlar (number, price...) → {@code java.lang.String} param
   *   <li>{@code mapSources} — {@code seller}/{@code buyer} (dotli token prefiksi) → {@code java.util.Map} param
   *   <li>{@code customFields} — {@code field:key} → {@code java.lang.String} param (template_field)
   * </ul>
   */
  public static void collectParams(
      String text, Set<String> scalar, Set<String> mapSources, Set<String> customFields) {
    if (text == null) {
      return;
    }
    Matcher m = PLACEHOLDER.matcher(text);
    while (m.find()) {
      String token = m.group(1);
      if (token.startsWith("field:")) {
        customFields.add(token.substring("field:".length()));
      } else if (token.contains(".")) {
        mapSources.add(token.substring(0, token.indexOf('.')));
      } else {
        scalar.add(token);
      }
    }
  }

  /** Matnni JRXML ifodaga aylantiradi. null/bo'sh → {@code ""}. */
  public static String compile(String text) {
    if (text == null || text.isEmpty()) {
      return "\"\"";
    }
    StringBuilder out = new StringBuilder();
    Matcher m = PLACEHOLDER.matcher(text);
    int last = 0;
    boolean first = true;
    while (m.find()) {
      if (m.start() > last) {
        first = append(out, literal(text.substring(last, m.start())), first);
      }
      first = append(out, expression(m.group(1)), first);
      last = m.end();
    }
    if (last < text.length()) {
      first = append(out, literal(text.substring(last)), first);
    }
    return out.isEmpty() ? "\"\"" : out.toString();
  }

  private static boolean append(StringBuilder out, String part, boolean first) {
    if (!first) {
      out.append(" + ");
    }
    out.append(part);
    return false;
  }

  // Token → JRXML ifoda.
  private static String expression(String token) {
    if (token.startsWith("field:")) {
      return "$P{" + token.substring("field:".length()) + "}";
    }
    int dot = token.indexOf('.');
    if (dot > 0) {
      String source = token.substring(0, dot);
      String key = token.substring(dot + 1);
      return "String.valueOf(((java.util.Map)$P{" + source + "}).get(\"" + key + "\"))";
    }
    // bare token — avto param. Noma'lum bo'lsa ham $P{} sifatida (compile-check ushlaydi).
    return "$P{" + token + "}";
  }

  // CDATA-xavfsiz Java string literali.
  private static String literal(String s) {
    String escaped =
        s.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\r", "")
            .replace("\n", "\\n");
    // CDATA ichida ]]> ketma-ketligini buzamiz.
    escaped = escaped.replace("]]>", "]]\" + \">");
    return "\"" + escaped + "\"";
  }

  public static boolean isAutoParam(String name) {
    return AUTO_PARAMS.contains(name);
  }

  public static Set<String> autoParamSet() {
    return new LinkedHashSet<>(AUTO_PARAMS);
  }
}
