package uz.hesap.service.integration.api.v1;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import uz.hesap.service.integration.model.LidRequest;
import uz.hesap.service.integration.service.GoogleSheetService;

// Landing'dan kelgan lidlar — faqat Google Sheets'ga yoziladi.
// Telegram bot integratsiyasi olib tashlangan (lid-bot kerakmas).
@Log4j2
@RestController
@RequestMapping("/integration/v1/lid")
@RequiredArgsConstructor
public class BotController {
  private final GoogleSheetService googleSheetSender;

  @PostMapping
  public Mono<Void> create(@RequestBody final LidRequest request) {
    return googleSheetSender.appendRow(request);
  }
}
