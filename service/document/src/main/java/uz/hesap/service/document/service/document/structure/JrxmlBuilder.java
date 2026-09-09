package uz.hesap.service.document.service.document.structure;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import uz.hesap.service.document.service.document.structure.model.TemplateStructure;
import uz.hesap.service.document.service.document.structure.model.TemplateStructure.Block;
import uz.hesap.service.document.service.document.structure.model.TemplateStructure.Column;
import uz.hesap.service.document.service.document.structure.model.TemplateStructure.Role;
import uz.hesap.service.document.service.document.structure.model.TemplateStructure.SignSide;

/**
 * Blok-struktura → JRXML generatori (zamonaviy "yangi ko'rinish" dizayni). Chiqishi mavjud
 * {@code DocumentGenerator} render kontraktiga mos: A4 (595×842, columnWidth 481, margin 57),
 * Times New Roman, avto $P paramlar (number/price/seller/buyer/paymentScheduleList...),
 * repeating jadval uchun jr:list + subDataset.
 *
 * <p>Tartib (gold-namuna {@code seed-c2c-sales-template.sql} bilan mos): property → style →
 * subDataset → parameter → title (bloklar) → summary (imzo/guvoh). Barcha uuid haqiqiy.
 */
public final class JrxmlBuilder {

  private static final int COL_W = 481;
  private static final int GAP = 8;
  // Rang olib tashlandi — hujjat monoxrom (qora). Accent/yashil endi qora.
  private static final String DEFAULT_ACCENT = "#000000";
  private static final String FONT = "Times New Roman";
  private static final String GRAY = "#666666";
  private static final String BORDER = "#D9D9D9";
  private static final String GREEN = "#000000";

  private final TemplateStructure s;
  private final String accent;

  private final StringBuilder body = new StringBuilder();
  private final List<String> subDatasets = new ArrayList<>();
  private final Set<String> scalarParams = new LinkedHashSet<>();
  private final Set<String> mapParams = new LinkedHashSet<>();
  private final Set<String> customFields = new LinkedHashSet<>();
  private final Set<String> listParams = new LinkedHashSet<>();

  // Yagona oqim kursori (detail band) — bloklar ketma-ket, positionType=Float bilan
  // sahifalararo oqadi (title/summary o'rniga detail: balandlik cheklovi yo'q).
  private int cy = 0;

  // Shablon entity'sidan keladigan sarlavha + taraf nomlari (struktura meta'sidan ustun).
  // Sarlavha = template.name*, 1/2-taraf nomi = template.first/secondName*.
  public record BuildContext(
      String titleUz,
      String titleRu,
      String titleEn,
      String firstUz,
      String firstRu,
      String firstEn,
      String secondUz,
      String secondRu,
      String secondEn,
      String productUz,
      String productRu,
      String productEn) {}

  private final BuildContext ctx;
  // Tanlangan til: 0=uz, 1=ru, 2=en. lang(uz,ru,en) shu indeks bo'yicha tanlaydi.
  private final int langIndex;

  private JrxmlBuilder(TemplateStructure s, BuildContext ctx, String lang) {
    this.s = s;
    this.ctx = ctx;
    this.langIndex = langIndexOf(lang);
    // Rang barcha shablonlar uchun bir xil — Hesap brend rangi (struktura'dan olinmaydi).
    this.accent = DEFAULT_ACCENT;
  }

  private static int langIndexOf(String lang) {
    if (lang == null) return 0;
    return switch (lang.toLowerCase()) {
      case "ru" -> 1;
      case "en", "en-gb" -> 2;
      default -> 0; // uz
    };
  }

  public static String build(TemplateStructure structure) {
    return new JrxmlBuilder(structure, null, "uz").render();
  }

  // Shablon konteksti bilan — sarlavha/taraf nomlari template'dan olinadi.
  public static String build(TemplateStructure structure, BuildContext ctx) {
    return new JrxmlBuilder(structure, ctx, "uz").render();
  }

  // Til bilan — sarlavha/matnlar tanlangan tilda (uz/ru/en) render qilinadi.
  public static String build(TemplateStructure structure, BuildContext ctx, String lang) {
    return new JrxmlBuilder(structure, ctx, lang).render();
  }

  private String render() {
    List<Block> blocks = s.blocks() == null ? List.of() : new ArrayList<>(s.blocks());
    // Guvohlar bloki har doim imzo (taraflar) blokidan OLDIN chiqadi —
    // strukturada keyin turgan bo'lsa ham oldinga ko'chiramiz.
    int wIdx = -1;
    int sIdx = -1;
    for (int i = 0; i < blocks.size(); i++) {
      Block bl = blocks.get(i);
      if (bl == null || bl.type() == null) continue;
      if (TemplateStructure.Type.WITNESSES.equals(bl.type())) wIdx = i;
      if (TemplateStructure.Type.SIGNATURE_BLOCK.equals(bl.type()) && sIdx < 0) sIdx = i;
    }
    if (wIdx >= 0 && sIdx >= 0 && wIdx > sIdx) {
      Block w = blocks.remove(wIdx);
      blocks.add(sIdx, w);
    } else if (wIdx < 0 && sIdx >= 0) {
      // Strukturada guvohlar bloki umuman yo'q — imzolagan guvohlar baribir
      // chiqishi uchun sintetik blok (bo'sh bo'lsa printWhen bilan yashiriladi).
      blocks.add(
          sIdx,
          new Block(
              null, TemplateStructure.Type.WITNESSES,
              null, null, null, null, null, null, null,
              Boolean.TRUE, null, null, null, null, null, null, null));
    }
    for (Block b : blocks) {
      if (b == null || b.type() == null) {
        continue;
      }
      switch (b.type()) {
        case TemplateStructure.Type.HEADER -> header();
        case TemplateStructure.Type.PARTY_BLOCK -> partyBlock();
        case TemplateStructure.Type.SECTION -> section(b);
        case TemplateStructure.Type.PRODUCT_TABLE -> {
          if (!Boolean.FALSE.equals(b.enabled()))
            repeatingTable(b, defaultProductColumns(b, ctx), "products");
        }
        case TemplateStructure.Type.CUSTOM_REPEATING_TABLE -> repeatingTable(b, b.columns(), "rows");
        case TemplateStructure.Type.PAYMENT_SCHEDULE -> {
          if (!Boolean.FALSE.equals(b.enabled())) paymentSchedule(b);
        }
        case TemplateStructure.Type.SIGNATURE_BLOCK -> signature(b);
        case TemplateStructure.Type.WITNESSES -> {
          if (!Boolean.FALSE.equals(b.enabled())) witnesses(b);
        }
        default -> {}
      }
    }
    return assemble();
  }

  // ---- Bloklar -------------------------------------------------------------

  private void header() {
    scalarParams.add("number");
    scalarParams.add("createdDate");
    int y = cy;
    // Logo + subtitle (chap)
    body.append(staticText(0, y, 240, 20, 16, true, "Left", accent, "HESAP"));
    body.append(
        staticText(
            0, y + 20, 260, 12, 8, false, "Left", GRAY,
            lang("Raqamli shartnoma platformasi", "Цифровая платформа договоров",
                "Digital contract platform")));
    // № + sana (o'ng)
    body.append(
        textField(
            281, y, 200, 14, 10, false, "Right", null, false,
            "\"" + lang("Shartnoma", "Договор", "Agreement") + " \\u2116 \" + $P{number}"));
    body.append(
        textField(
            281, y + 16, 200, 12, 9, false, "Right", GRAY, false,
            "\"" + lang("Sana", "Дата", "Date") + ": \" + $P{createdDate}"));
    // Binafsha chiziq
    body.append(rect(0, y + 36, COL_W, 2, accent, accent, true, 0));
    // Markazlashgan sarlavha
    String t = lang(metaTitle());
    body.append(staticText(0, y + 46, COL_W, 22, 13, true, "Center", null, t));
    cy =y + 76;
  }

  private void partyBlock() {
    Role first = roles() != null ? roles().first() : null;
    Role second = roles() != null ? roles().second() : null;
    int y = cy;
    // Taraf nomi template'dan (ctx) — bo'lmasa struktura meta'sidan.
    partyCard(0, y, 233, first, ctx == null ? null : lang(ctx.firstUz(), ctx.firstRu(), ctx.firstEn()));
    partyCard(248, y, 233, second, ctx == null ? null : lang(ctx.secondUz(), ctx.secondRu(), ctx.secondEn()));
    cy =y + 80;
  }

  // Chegara <frame stretchType="ContainerHeight"> bilan emas — bu amalda manzil
  // qisqa bo'lganda ham katta bo'sh joy qoldirib, kartani haddan tashqari
  // kattalashtirib yuborar edi. Buning o'rniga har bir maydonga alohida chap/o'ng
  // (va birinchisida yuqori, oxirgisida pastki) chiziq (box pen) chizamiz — ular
  // birga qo'shilib, aynan kontent balandligiga mos chegarani hosil qiladi.
  private void partyCard(int x, int y, int w, Role role, String overrideLabel) {
    String source = role != null && notBlank(role.source()) ? role.source() : "seller";
    mapParams.add(source);
    String label =
        notBlank(overrideLabel)
            ? overrideLabel
            : (role != null ? firstNonBlank(role.uz(), role.ru(), role.en()) : "");
    body.append(
        staticTextBoxed(
            x, y, w, 19, 8, true, "Left", accent, label.toUpperCase(),
            boxSides(true, true, false, true), 8, 8, 7, 0));
    body.append(
        textFieldBoxed(
            x, y + 19, w, 14, 10, true, "Left", null, true, mapGet(source, "fullName"),
            boxSides(false, true, false, true), 8, 8, 2, 0));
    body.append(
        textFieldBoxed(
            x, y + 33, w, 12, 8, false, "Left", GRAY, false,
            "\"" + lang("STIR/JSHSHIR", "ИНН/ПИНФЛ", "TIN/PINFL") + ": \" + " + mapGet(source, "in"),
            boxSides(false, true, false, true), 8, 8, 2, 0));
    // Manzil — FAQAT jismoniy shaxs uchun (yuridik shaxsda type=COMPANY -> bo'sh).
    // OneID manzili shaxsiy bo'lgani uchun kompaniyaga tegishli emas. Qancha qator
    // bo'lsa ham to'liq ko'rinishi uchun stretch=true — pastki chegara shu bilan suriladi.
    body.append(
        textFieldBoxed(
            x, y + 45, w, 16, 8, false, "Left", GRAY, true, addressIfIndividual(source),
            boxSides(false, true, true, true), 8, 8, 2, 7));
  }

  // Party manzili — yuridik shaxsda (type=COMPANY) bo'sh, aks holda address.
  private static String addressIfIndividual(String source) {
    return "\"COMPANY\".equals(" + mapGet(source, "type") + ") ? \"\" : " + mapGet(source, "address");
  }

  private void section(Block b) {
    int y = cy;
    String sTitle = lang(b.titleUz(), b.titleRu(), b.titleEn());
    int inner = 0;
    if (notBlank(sTitle)) {
      body.append(staticText(0, y, COL_W, 16, 10, true, "Left", null, sTitle));
      inner += 18;
    }
    String text = lang(b.bodyUz(), b.bodyRu(), b.bodyEn());
    PlaceholderCompiler.collectParams(text, scalarParams, mapParams, customFields);
    // Deklaratsiya balandligi kichik (band sahifaga sig'sin) — isStretchWithOverflow
    // runtime'da matn bo'yicha kengaytiradi, positionType=Float pastdagilarni suradi.
    int bodyH = 16;
    body.append(
        textField(
            0, y + inner, COL_W, bodyH, 10, false, align(b.align()), null, true,
            PlaceholderCompiler.compile(text)));
    cy = y + inner + bodyH + GAP;
  }

  private void repeatingTable(Block b, List<Column> columns, String defaultParentKey) {
    if (columns == null || columns.isEmpty()) {
      return;
    }
    String parentKey = notBlank(b.parentKey()) ? b.parentKey() : defaultParentKey;
    listParams.add(parentKey);
    int y = cy;
    String sTitle = lang(b.titleUz(), b.titleRu(), b.titleEn());
    if (notBlank(sTitle)) {
      body.append(staticText(0, y, COL_W, 16, 10, true, "Left", null, sTitle));
      y += 18;
    }
    int[] xs = columnX(columns);
    // Sarlavha qatori (binafsha shapka)
    for (int i = 0; i < columns.size(); i++) {
      Column c = columns.get(i);
      int w = xs[i + 1] - xs[i];
      body.append(
          headerCell(xs[i], y, w, lang(c.titleUz(), c.titleRu(), c.titleEn()), colAlign(c)));
    }
    // jr:list qatorlari
    StringBuilder cells = new StringBuilder();
    for (int i = 0; i < columns.size(); i++) {
      Column c = columns.get(i);
      int w = xs[i + 1] - xs[i];
      cells.append(
          bodyCell(xs[i] - xs[0], 0, w, colAlign(c), "$F{" + c.fieldKey() + "}"));
    }
    body.append(jrList(xs[0], y + 18, xs[columns.size()] - xs[0], parentKey, cells.toString()));
    subDatasets.add(subDataset(parentKey + "Dataset", fieldNames(columns)));
    cy =y + 18 + 16 + GAP;
  }

  private void paymentSchedule(Block b) {
    listParams.add("paymentScheduleList");
    int y = cy;
    String sTitle = lang(b.titleUz(), b.titleRu(), b.titleEn());
    if (!notBlank(sTitle)) {
      sTitle = lang("TO'LOV JADVALI", "ГРАФИК ПЛАТЕЖЕЙ", "PAYMENT SCHEDULE");
    }
    body.append(staticText(0, y, COL_W, 16, 10, true, "Left", null, sTitle));
    y += 18;
    // To'langan va Holat oxirida — o'quvchi avval nima/qachon to'lash kerakligini ko'rsin.
    String[] heads = {
      "№",
      lang("To'lov", "Платёж", "Payment"),
      lang("Muddat", "Срок", "Due date"),
      lang("To'langan", "Оплачено", "Paid"),
      lang("Holat", "Статус", "Status")
    };
    int[] xs = {0, 40, 150, 260, 370, 481};
    for (int i = 0; i < heads.length; i++) {
      body.append(headerCell(xs[i], y, xs[i + 1] - xs[i], heads[i], i == 0 ? "Center" : "Left"));
    }
    String[] keys = {"no", "toPay", "returnTime", "paidPart", "status"};
    StringBuilder cells = new StringBuilder();
    for (int i = 0; i < keys.length; i++) {
      cells.append(
          bodyCell(xs[i], 0, xs[i + 1] - xs[i], i == 0 ? "Center" : "Left", "$F{" + keys[i] + "}"));
    }
    body.append(jrList(0, y + 18, COL_W, "paymentScheduleList", cells.toString()));
    subDatasets.add(
        subDataset(
            "paymentScheduleListDataset",
            List.of("no", "toPay", "paidPart", "returnTime", "changeDate", "status", "paidTime")));
    cy =y + 18 + 16 + GAP;
  }

  private void signature(Block b) {
    int y = cy;
    signSide(0, y, 233, b.left(), b);
    signSide(248, y, 233, b.right(), b);
    cy = y + 96 + GAP;
  }

  // Footer taraf nomi template'dan: source="seller" → 1-taraf (ctx.first*),
  // source="buyer" → 2-taraf (ctx.second*). ctxOf: first*=sellerName, second*=buyerName.
  private String partyLabelForSource(String source) {
    if (ctx == null || source == null) {
      return null;
    }
    String s = source.toLowerCase();
    if (s.contains("seller")) {
      return lang(ctx.firstUz(), ctx.firstRu(), ctx.firstEn());
    }
    if (s.contains("buyer")) {
      return lang(ctx.secondUz(), ctx.secondRu(), ctx.secondEn());
    }
    return null;
  }

  private void signSide(int x, int y, int w, SignSide side, Block b) {
    if (side == null) {
      return;
    }
    if (notBlank(side.source())) {
      mapParams.add(side.source());
    }
    // Footer taraf nomi template'dan (source bo'yicha: seller→1-taraf, buyer→2-taraf);
    // ctx'da bo'lmasa SignSide'ning o'z labeliga tushadi.
    String label = partyLabelForSource(side.source());
    if (!notBlank(label)) {
      label = firstNonBlank(side.labelUz(), side.labelRu(), side.labelEn());
    }
    body.append(staticText(x, y, w, 12, 9, true, "Left", GRAY, label));
    String lines = firstNonBlank(side.linesUz(), side.linesRu(), side.linesEn());
    PlaceholderCompiler.collectParams(lines, scalarParams, mapParams, customFields);
    body.append(
        textField(x, y + 16, w, 48, 9, false, "Left", null, true, PlaceholderCompiler.compile(lines)));
    // Badge — haqiqiy tasdiqlash usuli (taraf turidan kelib chiqib backend
    // "verificationLabel" beradi: "SMS/MyID/E-IMZO bilan tasdiqlangan").
    // Manba bo'lsa dinamik textField; aks holda statik fallback (preset bayrog'i).
    if (notBlank(side.source())) {
      body.append(
          textField(
              x, y + 70, w, 12, 8, true, "Left", GREEN, true,
              mapGet(side.source(), "verificationLabel")));
    } else {
      String badge = badgeText(b);
      if (notBlank(badge)) {
        body.append(staticText(x, y + 70, w, 12, 8, true, "Left", GREEN, badge));
      }
    }
  }

  // Imzolagan (ACCEPTED) guvohlar rekvizitlari bilan jadval — taraflar imzosidan oldin.
  // Ma'lumot "witnessRows" param'idan keladi (DocumentGenerator to'ldiradi);
  // guvoh bo'lmasa faqat sarlavha qatori chiqadi.
  // Imzolagan (ACCEPTED) guvohlar rekvizitlari bilan jadval — taraflar imzosidan oldin.
  // Ma'lumot "witnessRows" param'idan (DocumentGenerator); bo'sh bo'lsa butun
  // seksiya printWhen bilan yashiriladi (joy ham olmaydi).
  private void witnesses(Block b) {
    listParams.add("witnessRows");
    String printWhen =
        "new Boolean($P{witnessRows} != null && !((java.util.Collection)$P{witnessRows}).isEmpty())";
    List<Column> columns =
        List.of(
            new Column(
                "witnessFullName",
                "Guvoh (F.I.Sh.)", "Свидетель (Ф.И.О.)", "Witness (full name)",
                201, "left"),
            new Column("witnessPinfl", "PINFL", "ПИНФЛ", "PINFL", 110, "center"),
            new Column("witnessPhone", "Telefon", "Телефон", "Phone", 100, "center"),
            new Column("witnessSignedDate", "Imzo sanasi", "Дата подписи", "Signed", 70, "center"));
    int y = cy;
    String title =
        lang(
            notBlank(b.titleUz()) ? b.titleUz() : "Guvohlar",
            notBlank(b.titleRu()) ? b.titleRu() : "Свидетели",
            notBlank(b.titleEn()) ? b.titleEn() : "Witnesses");
    body.append(withPrintWhen(staticText(0, y, COL_W, 16, 10, true, "Left", null, title), printWhen));
    y += 18;
    int[] xs = columnX(columns);
    for (int i = 0; i < columns.size(); i++) {
      Column c = columns.get(i);
      int w = xs[i + 1] - xs[i];
      body.append(
          withPrintWhen(
              headerCell(xs[i], y, w, lang(c.titleUz(), c.titleRu(), c.titleEn()), colAlign(c)),
              printWhen));
    }
    StringBuilder cells = new StringBuilder();
    for (int i = 0; i < columns.size(); i++) {
      Column c = columns.get(i);
      int w = xs[i + 1] - xs[i];
      cells.append(bodyCell(xs[i] - xs[0], 0, w, colAlign(c), "$F{" + c.fieldKey() + "}"));
    }
    body.append(
        withPrintWhen(
            jrList(xs[0], y + 18, xs[columns.size()] - xs[0], "witnessRows", cells.toString()),
            printWhen));
    subDatasets.add(subDataset("witnessRowsDataset", fieldNames(columns)));
    cy = y + 18 + 16 + GAP;
  }

  // Elementga printWhenExpression + isRemoveLineWhenBlank qo'shadi (bo'sh bo'lsa joy olmaydi).
  private static String withPrintWhen(String element, String expr) {
    // replaceFirst EMAS — expr ichida $P{...} bor, regex group-reference deb o'qiladi.
    int i = element.indexOf("/>");
    if (i < 0) {
      return element;
    }
    return element.substring(0, i)
        + " isRemoveLineWhenBlank=\"true\"><printWhenExpression><![CDATA["
        + expr
        + "]]></printWhenExpression></reportElement>"
        + element.substring(i + 2);
  }

  // ---- JRXML element helperlari -------------------------------------------

  private String staticText(
      int x, int y, int w, int h, int size, boolean bold, String align, String forecolor, String text) {
    return "<staticText><reportElement positionType=\"Float\" x=\""
        + x + "\" y=\"" + y + "\" width=\"" + w + "\" height=\"" + h + "\" uuid=\"" + uuid() + "\""
        + (forecolor != null ? " forecolor=\"" + forecolor + "\"" : "")
        + "/><textElement textAlignment=\"" + align + "\"><font fontName=\"" + FONT + "\" size=\""
        + size + "\" isBold=\"" + bold + "\"/></textElement><text><![CDATA[" + esc(text)
        + "]]></text></staticText>\n";
  }

  private String textField(
      int x, int y, int w, int h, int size, boolean bold, String align, String forecolor,
      boolean stretch, String expr) {
    return "<textField isStretchWithOverflow=\"" + stretch + "\" isBlankWhenNull=\"true\">"
        + "<reportElement positionType=\"Float\" x=\"" + x + "\" y=\"" + y + "\" width=\"" + w
        + "\" height=\"" + h + "\" uuid=\"" + uuid() + "\""
        + (forecolor != null ? " forecolor=\"" + forecolor + "\"" : "")
        + "/><textElement textAlignment=\"" + align + "\"><font fontName=\"" + FONT + "\" size=\""
        + size + "\" isBold=\"" + bold + "\"/></textElement><textFieldExpression><![CDATA[" + expr
        + "]]></textFieldExpression></textField>\n";
  }

  private String rect(
      int x, int y, int w, int h, String forecolor, String backcolor, boolean opaque, int unused) {
    return "<rectangle><reportElement positionType=\"Float\" x=\"" + x + "\" y=\"" + y + "\" width=\""
        + w + "\" height=\"" + h + "\" uuid=\"" + uuid() + "\""
        + (forecolor != null ? " forecolor=\"" + forecolor + "\"" : "")
        + (backcolor != null ? " backcolor=\"" + backcolor + "\"" : "")
        + " mode=\"" + (opaque ? "Opaque" : "Transparent") + "\"/>"
        + "<graphicElement><pen lineWidth=\"0.5\"/></graphicElement></rectangle>\n";
  }

  // "O'sadigan karta" texnikasi: bitta <frame stretchType="ContainerHeight"> o'rniga
  // har bir qatorga alohida chap/o'ng (kerak bo'lsa yuqori/pastki) chiziq qo'yamiz —
  // positionType="Float" bilan pastdagilar suriladi, chiziqlar esa aynan kontent
  // balandligiga mos uzunlikda qo'shiladi (frame + ContainerHeight amalda haddan
  // tashqari katta bo'sh joy qoldirib, kartani nomutanosib kattalashtirar edi).
  private String boxSides(boolean top, boolean left, boolean bottom, boolean right) {
    StringBuilder b = new StringBuilder("<box>");
    if (top) b.append("<topPen lineWidth=\"0.5\" lineColor=\"" + BORDER + "\"/>");
    if (left) b.append("<leftPen lineWidth=\"0.5\" lineColor=\"" + BORDER + "\"/>");
    if (bottom) b.append("<bottomPen lineWidth=\"0.5\" lineColor=\"" + BORDER + "\"/>");
    if (right) b.append("<rightPen lineWidth=\"0.5\" lineColor=\"" + BORDER + "\"/>");
    return b.append("</box>").toString();
  }

  private String staticTextBoxed(
      int x, int y, int w, int h, int size, boolean bold, String align, String forecolor,
      String text, String box, int padL, int padR, int padT, int padB) {
    return "<staticText><reportElement positionType=\"Float\" x=\"" + x + "\" y=\"" + y
        + "\" width=\"" + w + "\" height=\"" + h + "\" uuid=\"" + uuid() + "\""
        + (forecolor != null ? " forecolor=\"" + forecolor + "\"" : "") + "/>"
        + withPadding(box, padL, padR, padT, padB)
        + "<textElement textAlignment=\"" + align + "\"><font fontName=\"" + FONT + "\" size=\""
        + size + "\" isBold=\"" + bold + "\"/></textElement><text><![CDATA[" + esc(text)
        + "]]></text></staticText>\n";
  }

  private String textFieldBoxed(
      int x, int y, int w, int h, int size, boolean bold, String align, String forecolor,
      boolean stretch, String expr, String box, int padL, int padR, int padT, int padB) {
    return "<textField isStretchWithOverflow=\"" + stretch + "\" isBlankWhenNull=\"true\">"
        + "<reportElement positionType=\"Float\" x=\"" + x + "\" y=\"" + y + "\" width=\"" + w
        + "\" height=\"" + h + "\" uuid=\"" + uuid() + "\""
        + (forecolor != null ? " forecolor=\"" + forecolor + "\"" : "") + "/>"
        + withPadding(box, padL, padR, padT, padB)
        + "<textElement textAlignment=\"" + align + "\"><font fontName=\"" + FONT + "\" size=\""
        + size + "\" isBold=\"" + bold + "\"/></textElement><textFieldExpression><![CDATA[" + expr
        + "]]></textFieldExpression></textField>\n";
  }

  // box="<box>...</box>" ichiga padding atributlarini qo'shadi.
  private String withPadding(String box, int padL, int padR, int padT, int padB) {
    return box.replaceFirst(
        "<box>",
        "<box leftPadding=\"" + padL + "\" rightPadding=\"" + padR + "\" topPadding=\"" + padT
            + "\" bottomPadding=\"" + padB + "\">");
  }

  // Jadval sarlavha katagi — binafsha fon, oq matn.
  private String headerCell(int x, int y, int w, String text, String align) {
    return "<staticText><reportElement positionType=\"Float\" x=\"" + x + "\" y=\"" + y
        + "\" width=\"" + w
        + "\" height=\"18\" uuid=\"" + uuid() + "\" forecolor=\"#FFFFFF\" backcolor=\"" + accent
        + "\" mode=\"Opaque\"/><box><pen lineWidth=\"0.5\" lineColor=\"" + accent
        + "\"/></box><textElement textAlignment=\"" + align
        + "\" verticalAlignment=\"Middle\"><font fontName=\"" + FONT
        + "\" size=\"9\" isBold=\"true\"/></textElement><text><![CDATA[" + esc(text)
        + "]]></text></staticText>\n";
  }

  // jr:list ichidagi katak (box bilan to'r chizig'i).
  private String bodyCell(int x, int y, int w, String align, String expr) {
    return "<textField isBlankWhenNull=\"true\"><reportElement x=\"" + x + "\" y=\"" + y
        + "\" width=\"" + w + "\" height=\"16\" uuid=\"" + uuid()
        + "\"/><box><pen lineWidth=\"0.5\" lineColor=\"" + BORDER
        + "\"/></box><textElement textAlignment=\"" + align
        + "\" verticalAlignment=\"Middle\"><font fontName=\"" + FONT
        + "\" size=\"9\"/></textElement><textFieldExpression><![CDATA[" + expr
        + "]]></textFieldExpression></textField>\n";
  }

  private String jrList(int x, int y, int w, String parentKey, String contents) {
    return "<componentElement><reportElement positionType=\"Float\" x=\"" + x + "\" y=\"" + y
        + "\" width=\"" + w + "\" height=\"16\" uuid=\"" + uuid() + "\"/>"
        + "<jr:list xmlns:jr=\"http://jasperreports.sourceforge.net/jasperreports/components\""
        + " xsi:schemaLocation=\"http://jasperreports.sourceforge.net/jasperreports/components"
        + " http://jasperreports.sourceforge.net/xsd/components.xsd\">"
        + "<datasetRun subDataset=\"" + parentKey + "Dataset\"><dataSourceExpression><![CDATA["
        + "new net.sf.jasperreports.engine.data.JRMapCollectionDataSource($P{" + parentKey + "})"
        + "]]></dataSourceExpression></datasetRun>"
        + "<jr:listContents height=\"16\" width=\"" + w + "\">" + contents
        + "</jr:listContents></jr:list></componentElement>\n";
  }

  private String subDataset(String name, List<String> fields) {
    StringBuilder b = new StringBuilder();
    b.append("<subDataset name=\"").append(name).append("\" uuid=\"").append(uuid()).append("\">\n");
    for (String f : fields) {
      b.append("<field name=\"").append(f).append("\" class=\"java.lang.String\"/>\n");
    }
    b.append("</subDataset>\n");
    return b.toString();
  }

  // ---- Yig'ish --------------------------------------------------------------

  private String assemble() {
    StringBuilder x = new StringBuilder();
    x.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    x.append(
        "<jasperReport xmlns=\"http://jasperreports.sourceforge.net/jasperreports\""
            + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
            + " xsi:schemaLocation=\"http://jasperreports.sourceforge.net/jasperreports"
            + " http://jasperreports.sourceforge.net/xsd/jasperreport.xsd\""
            + " name=\"contract\" pageWidth=\"595\" pageHeight=\"842\" columnWidth=\"481\""
            + " leftMargin=\"57\" rightMargin=\"57\" topMargin=\"40\" bottomMargin=\"40\" uuid=\""
            + uuid() + "\">\n");
    x.append("<property name=\"net.sf.jasperreports.awt.ignore.missing.font\" value=\"true\"/>\n");
    for (String ds : subDatasets) {
      x.append(ds);
    }
    appendParams(x);
    // Bo'sh queryString — JREmptyDataSource (fillReport) bilan detail bir marta chiqadi.
    // Detail band balandlik cheklovisiz, splitType=Stretch bilan sahifalararo oqadi.
    x.append("<detail><band height=\"").append(Math.max(cy, 1)).append("\" splitType=\"Stretch\">\n");
    x.append(body);
    x.append("</band></detail>\n");
    x.append("</jasperReport>\n");
    return x.toString();
  }

  private void appendParams(StringBuilder x) {
    Set<String> scalars = new LinkedHashSet<>(scalarParams);
    // Header bo'lmasa ham bu paramlar zararsiz e'lon qilinadi (passed-extra ham OK).
    for (String p : scalars) {
      x.append("<parameter name=\"").append(p).append("\" class=\"java.lang.String\"/>\n");
    }
    for (String p : customFields) {
      if (!scalars.contains(p)) {
        x.append("<parameter name=\"").append(p).append("\" class=\"java.lang.String\"/>\n");
      }
    }
    for (String p : mapParams) {
      x.append("<parameter name=\"").append(p).append("\" class=\"java.util.Map\"/>\n");
    }
    for (String p : listParams) {
      x.append("<parameter name=\"").append(p).append("\" class=\"java.util.List\"/>\n");
    }
  }

  // ---- Yordamchilar ---------------------------------------------------------

  private TemplateStructure.Meta meta() {
    return s.meta();
  }

  private TemplateStructure.PartyRoles roles() {
    return meta() != null ? meta().partyRoles() : null;
  }

  private String[] metaTitle() {
    // Sarlavha template'dan (ctx.name*) — bo'lmasa struktura meta'sidan.
    if (ctx != null && notBlank(firstNonBlank(ctx.titleUz(), ctx.titleRu(), ctx.titleEn()))) {
      return new String[] {ctx.titleUz(), ctx.titleRu(), ctx.titleEn()};
    }
    return meta() == null
        ? new String[] {""}
        : new String[] {meta().titleUz(), meta().titleRu(), meta().titleEn()};
  }

  private String badgeText(Block b) {
    boolean e = Boolean.TRUE.equals(b.showEimzo());
    boolean m = Boolean.TRUE.equals(b.showMyId());
    if (e && m) {
      return lang(
          "E-IMZO / MyID bilan tasdiqlangan",
          "Подтверждено через E-IMZO / MyID",
          "Verified via E-IMZO / MyID");
    }
    if (e) {
      return lang(
          "E-IMZO bilan imzolangan", "Подписано через E-IMZO", "Signed via E-IMZO");
    }
    if (m) {
      return lang(
          "MyID bilan tasdiqlangan", "Подтверждено через MyID", "Verified via MyID");
    }
    return "";
  }

  private static List<Column> defaultProductColumns(Block b, BuildContext ctx) {
    if (b.columns() != null && !b.columns().isEmpty()) {
      return b.columns();
    }
    // Mahsulot ustuni sarlavhasi shablon productName'idan (taraf nomlaridek); bo'sh bo'lsa default.
    String pUz = ctx != null && notBlank(ctx.productUz()) ? ctx.productUz() : "Mahsulot nomi";
    String pRu = ctx != null && notBlank(ctx.productRu()) ? ctx.productRu() : "Наименование";
    String pEn = ctx != null && notBlank(ctx.productEn()) ? ctx.productEn() : "Name";
    return List.of(
        new Column("productName", pUz, pRu, pEn, 200, "left"),
        new Column("productUnit", "O'lchov", "Ед.", "Unit", 80, "center"),
        new Column("productAmount", "Miqdor", "Кол-во", "Qty", 70, "center"),
        new Column("productPrice", "Narx", "Цена", "Price", 65, "right"),
        new Column("productTotal", "Summa", "Сумма", "Total", 66, "right"));
  }

  private static List<String> fieldNames(List<Column> columns) {
    List<String> names = new ArrayList<>();
    for (Column c : columns) {
      names.add(c.fieldKey());
    }
    return names;
  }

  // Ustun x-koordinatalari (width berilmasa teng taqsimlanadi). Oxirgi element = jami eni.
  private static int[] columnX(List<Column> columns) {
    int n = columns.size();
    int[] xs = new int[n + 1];
    int total = 0;
    boolean hasWidth = columns.stream().anyMatch(c -> c.width() != null && c.width() > 0);
    if (hasWidth) {
      int sum = columns.stream().mapToInt(c -> c.width() == null ? 0 : c.width()).sum();
      double scale = sum > 0 ? (double) COL_W / sum : 1;
      for (int i = 0; i < n; i++) {
        xs[i] = total;
        int w = columns.get(i).width() == null ? 0 : columns.get(i).width();
        total += (int) Math.round(w * scale);
      }
    } else {
      int w = COL_W / n;
      for (int i = 0; i < n; i++) {
        xs[i] = total;
        total += w;
      }
    }
    xs[n] = COL_W;
    return xs;
  }

  private static String colAlign(Column c) {
    return align(c.align());
  }

  private static String mapGet(String source, String key) {
    return "String.valueOf(((java.util.Map)$P{" + source + "}).get(\"" + key + "\"))";
  }

  private static String align(String a) {
    if (a == null) {
      return "Left";
    }
    return switch (a.toLowerCase()) {
      case "justify", "justified" -> "Justified";
      case "center" -> "Center";
      case "right" -> "Right";
      default -> "Left";
    };
  }

  // Tanlangan til (langIndex) bo'yicha tanlaydi; bo'sh bo'lsa birinchi to'lgan qiymat.
  private String lang(String... v) {
    if (v != null && langIndex < v.length && notBlank(v[langIndex])) {
      return v[langIndex];
    }
    return firstNonBlank(v);
  }

  private static String firstNonBlank(String... values) {
    for (String v : values) {
      if (notBlank(v)) {
        return v;
      }
    }
    return "";
  }

  private static boolean notBlank(String v) {
    return v != null && !v.isBlank();
  }

  // CDATA-xavfsiz static text.
  private static String esc(String s) {
    return s == null ? "" : s.replace("]]>", "]]]]><![CDATA[>");
  }

  private static String uuid() {
    return UUID.randomUUID().toString();
  }
}
