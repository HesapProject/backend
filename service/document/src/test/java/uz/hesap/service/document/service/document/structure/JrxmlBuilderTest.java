package uz.hesap.service.document.service.document.structure;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import net.sf.jasperreports.engine.JasperCompileManager;
import org.junit.jupiter.api.Test;
import uz.hesap.service.document.service.document.structure.model.TemplateStructure;

/** JrxmlBuilder verifikatsiyasi: bo'sh struktura → JRXML → Jasper compile (XSD/CDATA tekshiruvi). */
class JrxmlBuilderTest {

  @Test
  void emptyStructureCompiles() {
    TemplateStructure empty = new TemplateStructure(1, null, null, List.of());
    assertDoesNotThrow(() -> compile(JrxmlBuilder.build(empty)));
  }

  private static void compile(String jrxml) throws Exception {
    try (InputStream in = new ByteArrayInputStream(jrxml.getBytes(StandardCharsets.UTF_8))) {
      JasperCompileManager.compileReport(in);
    }
  }
}
