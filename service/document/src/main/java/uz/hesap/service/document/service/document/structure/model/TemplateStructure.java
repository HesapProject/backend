package uz.hesap.service.document.service.document.structure.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Blok-konstruktor strukturasi — shablon JRXML manbasi. Admin Control'da bloklardan quradi,
 * {@code JrxmlBuilder} shundan JRXML generatsiya qiladi.
 *
 * <p>Barcha maydonlar nullable (Jackson) — blok turi ({@link Block#type()}) qaysi maydonlarni
 * ishlatishini belgilaydi. Bitta yassi {@link Block} record polimorf deserializatsiyani
 * o'rniga ishlatiladi (sodda + Jackson uchun xavfsiz).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TemplateStructure(Integer version, Page page, Meta meta, List<Block> blocks) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Page(String size, String accentColor) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Meta(
      String titleUz, String titleRu, String titleEn, PartyRoles partyRoles) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record PartyRoles(Role first, Role second) {}

  // source = "seller" | "buyer" — qaysi party Map'iga bog'lanishini bildiradi.
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Role(String uz, String ru, String en, String source) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Block(
      String id,
      String type,
      // section
      String titleUz,
      String titleRu,
      String titleEn,
      String bodyUz,
      String bodyRu,
      String bodyEn,
      String align,
      // productTable / paymentSchedule / customRepeatingTable
      Boolean enabled,
      String parentKey,
      List<Column> columns,
      // signatureBlock
      Boolean showEimzo,
      Boolean showMyId,
      SignSide left,
      SignSide right,
      // witnesses
      Integer count) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Column(
      String fieldKey, String titleUz, String titleRu, String titleEn, Integer width, String align) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record SignSide(
      String labelUz,
      String labelRu,
      String labelEn,
      String source,
      String linesUz,
      String linesRu,
      String linesEn) {}

  // Qo'llab-quvvatlanadigan blok turlari.
  public static final class Type {
    public static final String HEADER = "header";
    public static final String PARTY_BLOCK = "partyBlock";
    public static final String SECTION = "section";
    public static final String PRODUCT_TABLE = "productTable";
    public static final String PAYMENT_SCHEDULE = "paymentSchedule";
    public static final String SIGNATURE_BLOCK = "signatureBlock";
    public static final String WITNESSES = "witnesses";
    public static final String CUSTOM_REPEATING_TABLE = "customRepeatingTable";

    private Type() {}
  }
}
