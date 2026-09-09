package uz.hesap.service.integration.service;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import uz.hesap.service.integration.model.LidRequest;

@Log4j2
@Service
public class GoogleSheetService {

  private final Sheets sheets;
  private final String spreadsheetId;

  private static final String RANGE = "Sheet1!A:D";

  public GoogleSheetService(Sheets sheets, @Value("${google.sheet.id}") String spreadsheetId) {
    this.sheets = sheets;
    this.spreadsheetId = spreadsheetId;
  }

  public Mono<Void> appendRow(LidRequest request) {

    return Mono.fromRunnable(
            () -> {
              List<Object> row =
                  List.of(
                      request.businessType(),
                      request.name(),
                      request.phone(),
                      ZonedDateTime.now(ZoneId.of("Asia/Tashkent"))
                          .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

              ValueRange body = new ValueRange().setValues(List.of(row));

              try {
                sheets
                    .spreadsheets()
                    .values()
                    .append(spreadsheetId, RANGE, body)
                    .setValueInputOption("RAW")
                    .setInsertDataOption("INSERT_ROWS")
                    .execute();
              } catch (Exception e) {
                throw new RuntimeException(e);
              }
            })
        .subscribeOn(Schedulers.boundedElastic())
        .doOnError(e -> log.error("❌ Google Sheet append error", e))
        .then();
  }
}
