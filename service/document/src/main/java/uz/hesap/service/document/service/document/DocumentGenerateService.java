package uz.hesap.service.document.service.document;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.hesap.service.document.model.request.DocumentPreviewRequest;
import uz.hesap.service.document.service.c2c.ClaimsService;
import uz.hesap.service.document.service.c2c.NoticeService;

/** DocumentGenerateController doirasi — PDF generatsiya (shartnoma, ogohlantirish, da'vo, preview). */
@Service
@RequiredArgsConstructor
public class DocumentGenerateService {

  private final DocumentGenerator documentGenerator;
  private final NoticeService noticeService;
  private final ClaimsService claimService;

  public Mono<byte[]> generate(UUID documentId) {
    return documentGenerator.generate(documentId);
  }

  public Mono<byte[]> generate(UUID documentId, String lang) {
    return documentGenerator.generate(documentId, lang);
  }

  public Mono<byte[]> generateNotice(UUID noticeId) {
    return noticeService.generatePdf(noticeId);
  }

  public Mono<byte[]> generateReport(UUID reportId) {
    return claimService.generatePdf(reportId);
  }

  public Mono<byte[]> preview(DocumentPreviewRequest request, String creatorIn) {
    return documentGenerator.preview(request, creatorIn);
  }
}
