package uz.hesap.service.document.service.document.structure;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import uz.hesap.service.document.domain.enums.TemplateFieldType;
import uz.hesap.service.document.model.request.TemplateFieldRequest;
import uz.hesap.service.document.service.document.structure.model.TemplateStructure;
import uz.hesap.service.document.service.document.structure.model.TemplateStructure.Block;
import uz.hesap.service.document.service.document.structure.model.TemplateStructure.Column;
import uz.hesap.service.document.service.document.structure.model.TemplateStructure.SignSide;

/**
 * Strukturadan {@code template_field} qatorlarini ajratadi (document_value to'ldirishi uchun):
 *
 * <ul>
 *   <li>{@code {{field:key}}} (section/imzo matnida) → STRING field (parentKey=null)
 *   <li>productTable/customRepeatingTable ustunlari → LIST field (parentKey=block.parentKey)
 * </ul>
 *
 * <p>Avto-paramlar (number/price/seller/buyer/paymentScheduleList...) field YARATMAYDI —
 * DocumentGenerator ularni beradi. id=null qaytadi; servis mavjud field'lar bilan ID'ni
 * (parentKey|keyName bo'yicha) bog'laydi.
 */
public final class StructureFieldSync {

  private StructureFieldSync() {}

  public static List<TemplateFieldRequest> toRequests(TemplateStructure s) {
    List<TemplateFieldRequest> out = new ArrayList<>();
    Set<String> customKeys = new LinkedHashSet<>();
    Set<String> ignored = new LinkedHashSet<>();

    List<Block> blocks = s.blocks() == null ? List.of() : s.blocks();
    for (Block b : blocks) {
      if (b == null || b.type() == null) {
        continue;
      }
      switch (b.type()) {
        case TemplateStructure.Type.SECTION ->
            PlaceholderCompiler.collectParams(
                firstNonBlank(b.bodyUz(), b.bodyRu(), b.bodyEn()), ignored, ignored, customKeys);
        case TemplateStructure.Type.SIGNATURE_BLOCK -> {
          collectSign(b.left(), customKeys, ignored);
          collectSign(b.right(), customKeys, ignored);
        }
        case TemplateStructure.Type.PRODUCT_TABLE ->
            addColumns(out, columnsOrDefault(b), b.parentKey(), Boolean.TRUE);
        case TemplateStructure.Type.CUSTOM_REPEATING_TABLE ->
            addColumns(out, b.columns(), b.parentKey(), Boolean.FALSE);
        default -> {}
      }
    }

    for (String key : customKeys) {
      out.add(
          new TemplateFieldRequest(
              null, null, null, key, key, key, key, TemplateFieldType.STRING, null, Boolean.FALSE));
    }
    return out;
  }

  private static void collectSign(SignSide side, Set<String> custom, Set<String> ignored) {
    if (side == null) {
      return;
    }
    PlaceholderCompiler.collectParams(
        firstNonBlank(side.linesUz(), side.linesRu(), side.linesEn()), ignored, ignored, custom);
  }

  private static void addColumns(
      List<TemplateFieldRequest> out, List<Column> columns, String parentKey, Boolean productField) {
    if (columns == null || parentKey == null || parentKey.isBlank()) {
      return;
    }
    for (Column c : columns) {
      if (c.fieldKey() == null || c.fieldKey().isBlank()) {
        continue;
      }
      String label = firstNonBlank(c.titleUz(), c.titleRu(), c.titleEn(), c.fieldKey());
      out.add(
          new TemplateFieldRequest(
              null,
              parentKey,
              null,
              firstNonBlank(c.titleUz(), label),
              firstNonBlank(c.titleRu(), label),
              firstNonBlank(c.titleEn(), label),
              c.fieldKey(),
              TemplateFieldType.LIST,
              null,
              productField));
    }
  }

  // productTable ustunlari berilmasa default mahsulot ustunlari (JrxmlBuilder bilan mos).
  private static List<Column> columnsOrDefault(Block b) {
    if (b.columns() != null && !b.columns().isEmpty()) {
      return b.columns();
    }
    return List.of(
        new Column("productName", "Mahsulot nomi", "Наименование", "Name", 200, "left"),
        new Column("productUnit", "O'lchov", "Ед.", "Unit", 80, "center"),
        new Column("productAmount", "Miqdor", "Кол-во", "Qty", 70, "center"),
        new Column("productPrice", "Narx", "Цена", "Price", 65, "right"),
        new Column("productTotal", "Summa", "Сумма", "Total", 66, "right"));
  }

  private static String firstNonBlank(String... values) {
    for (String v : values) {
      if (v != null && !v.isBlank()) {
        return v;
      }
    }
    return "";
  }
}
