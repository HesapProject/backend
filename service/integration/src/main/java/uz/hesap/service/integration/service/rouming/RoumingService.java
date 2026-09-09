package uz.hesap.service.integration.service.rouming;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.exception.BadRequestException;
import uz.hesap.service.common.util.message.RoumingLogReply;
import uz.hesap.service.integration.model.rouming.RoumingFacturaDraftRequest;
import uz.hesap.service.integration.service.RoumingSettingService;
import uz.hesap.service.jms.JmsPublisher;

// Factura.uz (elektron schyot-faktura/ЭСФ) integratsiyasi. Oldi-berdi
// tasdiqlanganda sotuvchi nomidan ЭСФ import qilinadi:
//   POST {baseUrl}/Api/Document/ImportDocumentRegister?companyInn={sellerTin}
//   Authorization: Bearer <token>   (FacturaTokenService orqali)
//   body: { "invoices": [ { "head": {...}, "document": {...} } ] }
// Import qilingan hujjat Factura.uz kabinetida imzolanadi va yuboriladi.
// Xato bo'lsa error qaytadi — chaqiruvchi (document servisi) asosiy oqimni
// buzmasligi uchun onErrorResume bilan qamraydi. Har urinish log-servisga yoziladi.
@Log4j2
@Service
@RequiredArgsConstructor
public class RoumingService {

  // 860 = UZS (ISO 4217 raqamli kodi).
  private static final String UZS_CODE = "860";
  private static final String PROGRAM_VERSION = "1.0.0";
  private static final String FORMAT_VERSION = "1.0.0";

  private final WebClient.Builder webClientBuilder;
  private final RoumingSettingService settingService;
  private final FacturaTokenService tokenService;
  private final JmsPublisher jmsPublisher;
  private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

  // Sotuvchi nomidan ЭСФ import qilish. 401 kelsa token yangilanib bir marta
  // qayta uriniladi (token muddati o'tgan bo'lishi mumkin).
  public Mono<String> createSellerFacturaDraft(RoumingFacturaDraftRequest req) {
    if (req.sellerTin() == null || req.sellerTin().isBlank()) {
      return Mono.error(new BadRequestException("sellerTin is required"));
    }
    Map<String, Object> body = buildImportBody(req);
    String requestJson = toJson(body);

    return importFactura(req, body)
        .onErrorResume(
            UnauthorizedRetry.class,
            e -> {
              // Token eskirgan bo'lishi mumkin — keshni tozalab bir marta qayta urinamiz.
              tokenService.invalidate();
              return importFactura(req, body);
            })
        .doOnSuccess(
            respBody -> {
              log.info("Factura.uz ЭСФ import qilindi: {}", trim(respBody));
              sendRoumingLog(req, "SUCCESS", null, requestJson, respBody);
            })
        .doOnError(e -> sendRoumingLog(req, "ERROR", e.getMessage(), requestJson, null));
  }

  // Bitta import urinishi: token ol -> POST ImportDocumentRegister.
  private Mono<String> importFactura(RoumingFacturaDraftRequest req, Map<String, Object> body) {
    return Mono.zip(tokenService.accessToken(), settingService.getCurrent())
        .flatMap(
            tuple -> {
              String token = tuple.getT1();
              String baseUrl = tuple.getT2().getBaseUrl();
              if (baseUrl == null || baseUrl.isBlank()) {
                return Mono.error(
                    new BadRequestException(
                        "Factura.uz baseUrl sozlanmagan. Control -> Integratsiyalar -> Rouming."));
              }
              return webClientBuilder
                  .baseUrl(baseUrl)
                  .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                  .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                  .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                  .build()
                  .post()
                  .uri(
                      uriBuilder ->
                          uriBuilder
                              .path("/Api/Document/ImportDocumentRegister")
                              .queryParam("companyInn", req.sellerTin())
                              .build())
                  .bodyValue(body)
                  .retrieve()
                  .onStatus(
                      status -> status.value() == 401,
                      resp -> Mono.error(new UnauthorizedRetry()))
                  .onStatus(
                      status -> status.isError(),
                      resp ->
                          resp.bodyToMono(String.class)
                              .defaultIfEmpty("")
                              .flatMap(
                                  respBody -> {
                                    log.warn(
                                        "Factura.uz import failed: {} {}",
                                        resp.statusCode(),
                                        respBody);
                                    return Mono.error(
                                        new BadRequestException(
                                            "Factura.uz import failed: "
                                                + resp.statusCode()
                                                + " "
                                                + trim(respBody)));
                                  }))
                  .bodyToMono(String.class)
                  .defaultIfEmpty("");
            });
  }

  // ============ Hujjat quruvchi (Factura.uz ImportDocumentRegister sxemasi) ============

  private Map<String, Object> buildImportBody(RoumingFacturaDraftRequest req) {
    Map<String, Object> invoice = new LinkedHashMap<>();
    invoice.put("head", buildHead(req));
    invoice.put("document", buildDocument(req));
    Map<String, Object> root = new LinkedHashMap<>();
    root.put("invoices", List.of(invoice));
    return root;
  }

  private Map<String, Object> buildHead(RoumingFacturaDraftRequest req) {
    // file_name: sender_receiver_date_number (Factura.uz konvensiyasi).
    String fileName =
        String.join(
                "_",
                "invoice",
                nz(req.sellerTin()),
                nz(req.buyerTin()),
                nz(req.facturaDate()),
                nz(req.facturaNo()))
            + ".xml";

    Map<String, Object> senderInfo = new LinkedHashMap<>();
    senderInfo.put("INN", nz(req.sellerTin()));
    senderInfo.put("company_name", nz(req.sellerName()));

    Map<String, Object> receiverInfo = new LinkedHashMap<>();
    receiverInfo.put("INN", nz(req.buyerTin()));
    receiverInfo.put("company_name", nz(req.buyerName()));

    Map<String, Object> head = new LinkedHashMap<>();
    head.put("file_name", fileName);
    head.put("program_version", PROGRAM_VERSION);
    head.put("format_version", FORMAT_VERSION);
    head.put("sender", Map.of("sender_info", senderInfo));
    head.put("receiver", Map.of("receiver_info", receiverInfo));
    return head;
  }

  private Map<String, Object> buildDocument(RoumingFacturaDraftRequest req) {
    List<Map<String, Object>> items = new ArrayList<>();
    double subtotalSum = 0d;
    int ord = 1;
    for (RoumingFacturaDraftRequest.ProductLine p :
        req.products() == null
            ? List.<RoumingFacturaDraftRequest.ProductLine>of()
            : req.products()) {
      double count = p.count() == null ? 1d : p.count();
      double unitPrice = p.unitPrice() == null ? 0d : p.unitPrice();
      double lineTotal = p.totalSum() == null ? unitPrice * count : p.totalSum();
      subtotalSum += lineTotal;

      Map<String, Object> item = new LinkedHashMap<>();
      item.put("item_number", String.valueOf(ord++));
      item.put("description", nz(p.name()));
      item.put("volume", num(count));
      item.put("unit_price", num(unitPrice));
      item.put("subtotal", num(lineTotal));
      item.put("measurement_unit", "дона");
      // C2C/QQSsiz — vat 0. Yuridik QQS keyin kengaytiriladi.
      item.put("vat", Map.of("vat_rate", "0%", "vat_value", "0"));
      item.put("subtotal_with_taxes", num(lineTotal));
      item.put("excise", Map.of("excise_rate", 0, "excise_value", 0));
      items.add(item);
    }

    Map<String, Object> summary = new LinkedHashMap<>();
    summary.put("column_subtotal", num(subtotalSum));
    summary.put("column_subtotal_uzs", num(subtotalSum));
    summary.put("column_vat_value", "0");
    summary.put("column_vat_value_uzs", "0");
    summary.put("column_subtotal_with_taxes", num(subtotalSum));
    summary.put("column_subtotal_with_taxes_uzs", num(subtotalSum));

    Map<String, Object> doc = new LinkedHashMap<>();
    doc.put("document_number", nz(req.facturaNo()));
    doc.put("document_date", nz(req.facturaDate()));
    doc.put("contract_number", nz(req.contractNo()));
    doc.put("contract_date", nz(req.contractDate()));
    doc.put("currency_code", UZS_CODE);
    doc.put("currency_rate", "1");
    doc.put("items", items);
    doc.put("column_summary_values", summary);
    return doc;
  }

  // ============ Log ============

  private void sendRoumingLog(
      RoumingFacturaDraftRequest req,
      String status,
      String errorMessage,
      String request,
      String response) {
    jmsPublisher
        .publish(
            new RoumingLogReply(
                req.contractId(),
                req.contractNo(),
                req.facturaNo(),
                req.sellerTin(),
                req.buyerTin(),
                status,
                errorMessage,
                request,
                response,
                java.time.Instant.now()))
        .subscribe();
  }

  // ============ Helpers ============

  private String toJson(final Object value) {
    if (value == null) return null;
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception e) {
      return null;
    }
  }

  // Soliq formati: butun bo'lsa "367500", kasrli bo'lsa 2 xona ("367500.00").
  private static String num(double v) {
    if (v == Math.floor(v) && !Double.isInfinite(v)) {
      return String.valueOf((long) v);
    }
    return String.format(java.util.Locale.US, "%.2f", v);
  }

  private static String nz(String v) {
    return v == null ? "" : v;
  }

  private static String trim(String v) {
    if (v == null) return "";
    return v.length() > 300 ? v.substring(0, 300) : v;
  }

  // 401 -> token yangilash uchun ichki signal.
  private static final class UnauthorizedRetry extends RuntimeException {
    UnauthorizedRetry() {
      super("Factura.uz 401 — token yangilanadi");
    }
  }
}
