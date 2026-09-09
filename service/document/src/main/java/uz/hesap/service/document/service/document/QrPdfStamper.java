package uz.hesap.service.document.service.document;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

// Jasper chiqargan shartnoma PDF'ining oxirgi sahifasi pastki-o'ng burchagiga
// QR (contract.hesap.uz/{id}) + 4 xonali kirish kodini overlay qiladi. Xatolik bo'lsa
// asl PDF qaytadi (PDF hech qachon buzilmasin) — repository.gov.uz uslubidagi ko'rinish.
@Slf4j
public final class QrPdfStamper {

  private static final String BASE_URL = "https://contract.hesap.uz/";

  private QrPdfStamper() {}

  public static byte[] stamp(byte[] pdf, UUID contractId, String accessCode) {
    if (pdf == null || pdf.length == 0 || contractId == null) return pdf;
    try (PDDocument doc = PDDocument.load(pdf)) {
      byte[] qrPng = qrPng(BASE_URL + contractId, 240);
      PDImageXObject qr = PDImageXObject.createFromByteArray(doc, qrPng, "qr");

      PDPage page = doc.getPage(doc.getNumberOfPages() - 1);
      float pageW = page.getMediaBox().getWidth();
      float margin = 24f;
      float qrSize = 68f;
      float qrX = pageW - margin - qrSize;
      float qrY = margin;

      String code = (accessCode == null || accessCode.isBlank()) ? "----" : accessCode;
      float boxW = 34f;
      float boxX = qrX - boxW - 6f;
      float boxY = qrY;

      try (PDPageContentStream cs =
          new PDPageContentStream(doc, page, AppendMode.APPEND, true, true)) {
        // QR
        cs.drawImage(qr, qrX, qrY, qrSize, qrSize);

        // "PIN" yorlig'i (kichik, QR chapida — ramkasiz)
        cs.beginText();
        cs.setNonStrokingColor(120, 120, 120);
        cs.setFont(PDType1Font.HELVETICA, 6f);
        float labelW = PDType1Font.HELVETICA.getStringWidth("PIN") / 1000f * 6f;
        cs.newLineAtOffset(boxX + (boxW - labelW) / 2f, boxY + qrSize / 2f + 4f);
        cs.showText("PIN");
        cs.endText();

        // 4 xonali kod (markazda, qalin)
        cs.beginText();
        cs.setNonStrokingColor(17, 24, 39);
        cs.setFont(PDType1Font.HELVETICA_BOLD, 13f);
        float textW = PDType1Font.HELVETICA_BOLD.getStringWidth(code) / 1000f * 13f;
        cs.newLineAtOffset(boxX + (boxW - textW) / 2f, boxY + qrSize / 2f - 10f);
        cs.showText(code);
        cs.endText();
      }

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      doc.save(out);
      return out.toByteArray();
    } catch (Exception e) {
      log.warn("QR stamp failed for contract {}: {}", contractId, e.getMessage());
      return pdf;
    }
  }

  private static byte[] qrPng(String content, int size) throws Exception {
    QRCodeWriter writer = new QRCodeWriter();
    BitMatrix matrix =
        writer.encode(
            content,
            BarcodeFormat.QR_CODE,
            size,
            size,
            Map.of(EncodeHintType.MARGIN, 1, EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M));
    BufferedImage img = MatrixToImageWriter.toBufferedImage(matrix);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ImageIO.write(img, "PNG", baos);
    return baos.toByteArray();
  }
}
