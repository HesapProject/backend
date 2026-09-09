package uz.hesap.service.integration.service.katm;

import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.hesap.service.integration.domain.enums.KatmReportStatus;
import uz.hesap.service.integration.repository.KatmReportRepository;

// REQUESTED holatdagi KATM hisobotlarini har 60 sekundda tekshiradi —
// get-report orqali reportBase64 kelsa COMPLETED qiladi. PlumSchedulerService
// bilan bir xil yondashuv.
@Log4j2
@Service
public class KatmSchedulerService {

  private final KatmReportRepository reportRepository;
  private final KatmService katmService;

  public KatmSchedulerService(KatmReportRepository reportRepository, KatmService katmService) {
    this.reportRepository = reportRepository;
    this.katmService = katmService;
  }

  @Scheduled(fixedRate = 60000)
  public void checkPendingReports() {
    reportRepository
        .findAllByStatus(KatmReportStatus.REQUESTED)
        .flatMap(katmService::pollPendingReport)
        .onErrorResume(
            e -> {
              // Creds sozlanmagan yoki KATM ishlamasa — keyingi tick'da yana urinadi.
              log.warn("KATM scheduler skipped: {}", e.getMessage());
              return Mono.empty();
            })
        .subscribe();
  }
}
