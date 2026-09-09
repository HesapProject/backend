package uz.hesap.service.document.service.document;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.sf.jasperreports.engine.*;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import uz.hesap.service.common.exception.NotFoundException;
import uz.hesap.service.common.util.UserPassportBasicResponse;
import uz.hesap.service.document.domain.document.DocumentEntity;
import uz.hesap.service.document.domain.document.DocumentValueEntity;
import uz.hesap.service.document.domain.enums.Currency;
import uz.hesap.service.document.domain.enums.DocumentPartyStatus;
import uz.hesap.service.document.domain.enums.ExchangeMode;
import uz.hesap.service.document.domain.enums.VerificationType;
import uz.hesap.service.document.util.NumberToWords;
import uz.hesap.service.document.domain.payment.PaidScheduleEntity;
import uz.hesap.service.document.domain.payment.PaymentEntity;
import uz.hesap.service.document.domain.template.TemplateEntity;
import uz.hesap.service.document.domain.template.TemplateFieldEntity;
import uz.hesap.service.document.model.request.ContractProductRequest;
import uz.hesap.service.document.model.request.DocumentPreviewRequest;
import uz.hesap.service.document.repository.*;
import uz.hesap.service.document.domain.document.ContractProductEntity;
import uz.hesap.service.document.domain.document.ProductRequestEntity;
import uz.hesap.service.document.domain.enums.ProductRequestStatus;
import uz.hesap.service.document.domain.enums.ProductUnit;
import uz.hesap.service.document.webclient.FileServiceClient;
import uz.hesap.service.document.webclient.UserServiceClient;

@Log4j2
@Service
@RequiredArgsConstructor
public class DocumentGenerator {
  private final DocumentRepository documentRepository;
  private final uz.hesap.service.document.repository.CustomDocumentRepository
      customDocumentRepository;
  private final DocumentValueRepository documentValueRepository;
  private final TemplateRepository templateRepository;
  private final TemplateFieldRepository templateFieldRepository;
  private final PaymentScheduleRepository paymentScheduleRepository;
  private final PaidScheduleRepository paidScheduleRepository;
  private final UserServiceClient userServiceClient;
  private final uz.hesap.service.document.service.template.TemplateStructureService
      templateStructureService;
  private final FileServiceClient fileServiceClient;
  private final ContractProductRepository contractProductRepository;
  private final uz.hesap.service.document.repository.WitnessRequestRepository
      witnessRequestRepository;
  private final ProductRequestRepository productRequestRepository;

  // PDF'dan rang olib tashlanadi — brend ranglari (accent/yashil) qora'ga almashtiriladi.
  // Barcha shablonlar (mavjud stored templateData + yangi) uchun render vaqtida qo'llanadi.
  private static String stripColors(final String jrxml) {
    if (jrxml == null) {
      return null;
    }
    return jrxml
        .replaceAll("(?i)#534AB7", "#000000") // accent (binafsha) → qora
        .replaceAll("(?i)#0F6E56", "#000000"); // tasdiq badge (yashil) → qora
  }

  public Mono<byte[]> generatePdf(String jrxml, Map<String, Object> parameters) {
    final String monochrome = stripColors(jrxml);
    return Mono.fromCallable(
            () -> {
              try (InputStream jrxmlStream =
                  new ByteArrayInputStream(monochrome.getBytes(StandardCharsets.UTF_8))) {

                JasperReport jasperReport = JasperCompileManager.compileReport(jrxmlStream);

                JasperPrint jasperPrint =
                    JasperFillManager.fillReport(jasperReport, parameters, new JREmptyDataSource());

                return JasperExportManager.exportReportToPdf(jasperPrint);
              }
            })
        .subscribeOn(Schedulers.boundedElastic());
  }

  public Mono<byte[]> generate(final UUID documentId) {
    return generate(documentId, null);
  }

  // lang: uz/ru/en — foydalanuvchi ilova tilida ko'rmoqchi bo'lgan PDF. Kesh (pdfUrl*)
  // HAR BIR til uchun ALOHIDA — bitta hujjatning uz/ru/en PDF'lari bir-birini
  // almashtira olmaydi, shuning uchun uch xil kesh maydoni ishlatiladi.
  public Mono<byte[]> generate(final UUID documentId, final String lang) {
    String normalizedLang = normalizeLang(lang);
    return documentRepository
        .findById(documentId)
        .flatMap(
            document -> {
              // Cache hit: shu til uchun URL bor va lastModifiedDate'dan keyin generatsiya qilingan
              if (isCacheValid(document, normalizedLang)) {
                log.debug("PDF cache hit for document {} lang={}", documentId, normalizedLang);
                return fileServiceClient.downloadPdfBytes(cachedUrl(document, normalizedLang));
              }
              // Cache miss: yangi PDF generatsiya, QR+kod overlay, upload, URL saqlash
              return generateFresh(document, normalizedLang)
                  .map(
                      pdf ->
                          QrPdfStamper.stamp(pdf, document.getId(), document.getAccessCode()))
                  .flatMap(
                      pdfBytes ->
                          fileServiceClient
                              .uploadPdfBytes(
                                  pdfBytes,
                                  "contract-" + document.getId() + "-" + normalizedLang + ".pdf")
                              .flatMap(
                                  url -> {
                                    applyCache(document, normalizedLang, url, Instant.now());
                                    return documentRepository
                                        .save(document)
                                        .thenReturn(pdfBytes);
                                  })
                              // upload xatolik bo'lsa ham PDF bytes'ni qaytaramiz
                              .onErrorResume(
                                  e -> {
                                    log.warn(
                                        "PDF cache upload failed for {} lang={}: {}",
                                        documentId,
                                        normalizedLang,
                                        e.getMessage());
                                    return Mono.just(pdfBytes);
                                  }));
            });
  }

  // Qo'llab-quvvatlanmaydigan/bo'sh qiymat → "uz" (default).
  private static String normalizeLang(String lang) {
    if (lang == null || lang.isBlank()) return "uz";
    return switch (lang.toLowerCase()) {
      case "ru", "en" -> lang.toLowerCase();
      default -> "uz";
    };
  }

  private static String cachedUrl(DocumentEntity d, String lang) {
    return switch (lang) {
      case "ru" -> d.getPdfUrlRu();
      case "en" -> d.getPdfUrlEn();
      default -> d.getPdfUrl();
    };
  }

  private static Instant cachedGeneratedAt(DocumentEntity d, String lang) {
    return switch (lang) {
      case "ru" -> d.getPdfGeneratedAtRu();
      case "en" -> d.getPdfGeneratedAtEn();
      default -> d.getPdfGeneratedAt();
    };
  }

  private static void applyCache(DocumentEntity d, String lang, String url, Instant at) {
    switch (lang) {
      case "ru" -> {
        d.setPdfUrlRu(url);
        d.setPdfGeneratedAtRu(at);
      }
      case "en" -> {
        d.setPdfUrlEn(url);
        d.setPdfGeneratedAtEn(at);
      }
      default -> {
        d.setPdfUrl(url);
        d.setPdfGeneratedAt(at);
      }
    }
  }

  // ============ Child hujjatlar (talabnoma / da'vo arizasi) PDF ============
  // NOTICE/REPORT shablonlari $P{...} larni FLAT String sifatida kutadi
  // (shartnoma generate'idagi seller/buyer Map'lari emas). Paramlar ota-hujjat
  // (document) datasidan olinadi; JRXML esa notice/report klonlangan templateJson.
  // noticeNumber yoki reportNumber'dan biri beriladi (ikkinchisi null).
  public Mono<byte[]> generateChild(
      final UUID documentId,
      final String jrxml,
      final Integer noticeNumber,
      final Integer reportNumber) {
    return documentRepository
        .findById(documentId)
        .switchIfEmpty(Mono.error(new NotFoundException("Document not found")))
        .flatMap(
            document -> {
              Mono<UserPassportBasicResponse> seller =
                  fetchPartyByIn(document.getSellerIn()).defaultIfEmpty(emptyParty());
              Mono<UserPassportBasicResponse> buyer =
                  fetchPartyByIn(document.getBuyerIn()).defaultIfEmpty(emptyParty());
              return Mono.zip(seller, buyer)
                  .flatMap(
                      t -> {
                        // seller = haqdor (qarz beruvchi = creditor), buyer = qarzdor (debtor)
                        String sellerName = nullToEmpty(t.getT1().fullName());
                        String buyerName = nullToEmpty(t.getT2().fullName());
                        Map<String, Object> p = new LinkedHashMap<>();
                        p.put("sellerFullName", sellerName);
                        p.put("creditorFullName", sellerName);
                        p.put("buyerFullName", buyerName);
                        p.put("debtorFullName", buyerName);
                        p.put("number", numberWithN(document.getNumber()));
                        p.put("price", formatMoney(toSom(document.getPrice())));
                        p.put(
                            "currency",
                            document.getCurrency() != null ? document.getCurrency().name() : "");
                        p.put("createdDate", fmt(document.getCreatedDate(), ZoneId.of("UTC+5")));
                        if (noticeNumber != null) {
                          p.put("noticeNumber", String.valueOf(noticeNumber));
                        }
                        if (reportNumber != null) {
                          p.put("reportNumber", String.valueOf(reportNumber));
                        }
                        return generatePdf(jrxml, p);
                      });
            });
  }

  // Summani bo'shliq bilan guruhlangan butun son String'iga aylantiradi: 3000000 -> "3 000 000".
  private static String formatMoney(Double amount) {
    long whole = amount == null ? 0L : (long) Math.floor(amount);
    java.text.DecimalFormatSymbols sym = new java.text.DecimalFormatSymbols(java.util.Locale.US);
    sym.setGroupingSeparator(' ');
    return new java.text.DecimalFormat("#,##0", sym).format(whole);
  }

  // Cache haqiqiyligini tekshirish (shu til uchun):
  //  - o'sha tilning pdfUrl* bor bo'lishi kerak
  //  - pdfGeneratedAt* >= lastModifiedDate (yo'q bo'lsa stale)
  // PDF layout o'zgarganda eski keshlar yaroqsiz sanaladi (guvohlar jadvali qo'shilgan sana).
  private static final Instant LAYOUT_CHANGED_AT = Instant.parse("2026-07-18T18:00:00Z");

  private static boolean isCacheValid(DocumentEntity document, String lang) {
    String url = cachedUrl(document, lang);
    Instant generatedAt = cachedGeneratedAt(document, lang);
    if (url == null || generatedAt == null) {
      return false;
    }
    if (generatedAt.isBefore(LAYOUT_CHANGED_AT)) {
      return false;
    }
    Instant lastMod = document.getLastModifiedDate();
    if (lastMod == null) {
      return true; // hech qachon o'zgartirilmagan, cache OK
    }
    return !generatedAt.isBefore(lastMod);
  }

  // Cache miss case: DB'dan barcha ma'lumotni olib Jasper render qilish. lang — normalizeLang()
  // orqali kelgani uchun har doim "uz"/"ru"/"en" (hech qachon null emas).
  private Mono<byte[]> generateFresh(DocumentEntity document, String lang) {
    UUID documentId = document.getId();
    return templateRepository
        .findById(document.getTemplateId())
        .flatMap(
            template ->
                Mono.zip(
                        documentValueRepository
                            .findAllByDocumentIdAndDeletedFalse(documentId)
                            .collectList(),
                        templateFieldRepository
                            .findAllByTemplateIdAndDeletedFalse(document.getTemplateId())
                            .collectList(),
                        paymentScheduleRepository
                            .findAllByContractIdAndDeletedFalse(documentId)
                            .collectList(),
                        paidScheduleRepository
                            .findAllByDocumentIdAndDeletedFalse(documentId)
                            .collectList(),
                        contractProductRepository
                            .findAllByDocumentIdAndDeletedFalse(documentId)
                            .collectList(),
                        productRequestRepository
                            .findAllByContractIdAndDeletedFalseOrderByCreatedDateDesc(documentId)
                            .collectList(),
                        witnessRequestRepository.findAllByContractId(documentId).collectList())
                    .flatMap(
                        tuple -> {
                          List<DocumentValueEntity> documentValue = tuple.getT1();
                          List<TemplateFieldEntity> templateField = tuple.getT2();
                          boolean isMoneyMode = template.getExchangeMode() == ExchangeMode.MONEY;
                          Map<String, Object> params =
                              toMap(
                                  document,
                                  documentValue,
                                  templateField,
                                  tuple.getT3(),
                                  tuple.getT4(),
                                  tuple.getT5(),
                                  tuple.getT6(),
                                  isMoneyMode,
                                  lang);
                          return buildWitnessRows(tuple.getT7())
                              .doOnNext(rows -> params.put("witnessRows", rows))
                              .then(enrichParams(document, params, lang))
                              .flatMap(
                                  enriched ->
                                      renderWithTemplate(
                                          template,
                                          enriched,
                                          document.getSellerStatus()
                                              == DocumentPartyStatus.ACCEPTED,
                                          document.getBuyerStatus()
                                              == DocumentPartyStatus.ACCEPTED,
                                          lang));
                        }));
  }

  // YAGONA render yo'li — ham saqlangan shartnoma (generateFresh), ham preview
  // (renderPreview) shu metoddan o'tadi, natija doim bir xil bo'ladi (kelajakda ikki
  // yo'l bir-biridan farq qilib qolmasin). params allaqachon to'ldirilgan (party'lar
  // enrich qilingan). sellerSigned/buyerSigned imzo badge'ini boshqaradi (preview'da false).
  private Mono<byte[]> renderWithTemplate(
      TemplateEntity template,
      Map<String, Object> params,
      boolean sellerSigned,
      boolean buyerSigned,
      String lang) {
    String jrxml = resolveJrxml(template, lang);
    return generatePdf(
        jrxml, addVerificationLabels(params, template, sellerSigned, buyerSigned, lang));
  }

  // Avval shu tilda saqlangan compiled JRXML (templateDataUz/Ru/En — saveStructure/
  // rebuildAllJrxml paytida oldindan tayyorlanadi). Bo'sh bo'lsa (struktura yaqinda
  // qo'shilgan tarjimasiz shablon) — runtime'da struktura'dan qayta quriladi (sekinroq,
  // lekin doim to'g'ri natija beradi); u ham bo'lmasa oxirgi chora — uz.
  private String resolveJrxml(TemplateEntity template, String lang) {
    // Tildan qat'i nazar, avval o'sha tildagi saqlangan JRXML — bo'lmasa Uz fallback.
    String stored = pickStoredJrxml(template, lang);
    if (stored != null && !stored.isBlank()) {
      return stored;
    }
    if (lang != null && !lang.isBlank() && !"uz".equalsIgnoreCase(lang)) {
      String jrxml = templateStructureService.buildJrxmlForLang(template, lang);
      if (jrxml != null) return jrxml;
    }
    return template.getTemplateDataUz();
  }

  // Tilga to'g'ri kelgan saqlangan JRXML — bo'lmasa null (fallback resolveJrxml tarafida).
  private static String pickStoredJrxml(TemplateEntity t, String lang) {
    if (lang == null) return t.getTemplateDataUz();
    return switch (lang.toLowerCase()) {
      case "ru" -> t.getTemplateDataRu();
      case "en" -> t.getTemplateDataEn();
      default -> t.getTemplateDataUz();
    };
  }

  // Universal "party" Map'lari: $P{seller} va $P{buyer} — har biri quyidagi key'larga ega:
  //   fullName, firstName, lastName, midName, phone, pinfl, passport, document,
  //   legalName, tin, address, type, isVerified
  // Template muallifi kerakli'sini chaqiradi: $P{seller}.get("legalName").
  // sellerUserId = haqdor, buyerUserId = qarzdor (qarz shartnomalari uchun).
  private Mono<Map<String, Object>> enrichParams(
      DocumentEntity document, Map<String, Object> params, String lang) {
    applyDocumentFields(params, document, lang);

    Mono<UserPassportBasicResponse> seller =
        fetchPartyByIn(document.getSellerIn()).defaultIfEmpty(emptyParty());
    Mono<UserPassportBasicResponse> buyer =
        fetchPartyByIn(document.getBuyerIn()).defaultIfEmpty(emptyParty());

    return Mono.zip(seller, buyer)
        .map(
            tuple -> {
              params.put("seller", toPartyMap(tuple.getT1()));
              params.put("buyer", toPartyMap(tuple.getT2()));
              if (!params.containsKey("address") || isBlank(params.get("address"))) {
                params.put("address", firstNonBlank(tuple.getT1().address(), "Toshkent shahri"));
              }
              return params;
            });
  }

  private Mono<UserPassportBasicResponse> fetchParty(UUID userId) {
    if (userId == null) {
      return Mono.empty();
    }
    return userServiceClient
        .getPartyPassportBasic(userId)
        .onErrorResume(
            e -> {
              log.warn("Party passport fetch failed for userId={}: {}", userId, e.getMessage());
              return userServiceClient.getUserById(userId).map(this::fromUserResponse);
            });
  }

  // Taraf PINFL/STIR bo'yicha (shartnoma buyer_in/seller_in) — UUID saqlanmaydi.
  private Mono<UserPassportBasicResponse> fetchPartyByIn(String in) {
    if (in == null || in.isBlank()) {
      return Mono.empty();
    }
    return userServiceClient
        .getUserByIn(in)
        .map(this::fromUserResponse)
        .onErrorResume(
            e -> {
              log.warn("Party passport fetch failed for in={}: {}", in, e.getMessage());
              return Mono.empty();
            });
  }

  // Fallback konstruktor (direktor ma'lumotisiz — main servisdan olib bo'lmasa).
  // Owner F.I.SH main servisdagi getPartyPassportBasic'dan keladi; bu yerda esa
  // faqat oddiy UserResponse'dan yig'iladigan fallback yo'l, direktor bo'sh.
  private UserPassportBasicResponse fromUserResponse(
      uz.hesap.service.common.util.UserResponse user) {
    return new UserPassportBasicResponse(
        user.id(),
        buildFullName(user),
        nullToEmpty(user.firstName()),
        nullToEmpty(user.lastName()),
        nullToEmpty(user.midName()),
        nullToEmpty(user.document()),
        nullToEmpty(user.in()),
        nullToEmpty(user.phone()),
        nullToEmpty(user.address()),
        nullToEmpty(user.legalName()),
        nullToEmpty(user.tin()),
        "",
        "",
        "",
        "",
        user.type(),
        Boolean.TRUE.equals(user.verified()));
  }

  private static UserPassportBasicResponse emptyParty() {
    return new UserPassportBasicResponse(
        null, "", "", "", "", "", "", "", "", "", "", "", "", "", "", null, Boolean.FALSE);
  }

  // Shartnoma raqami "N" prefiksi bilan (title uchun): "N 250622-0001". Bo'sh bo'lsa "".
  private static String numberWithN(String number) {
    return (number != null && !number.isBlank()) ? "N " + number : "";
  }

  private static void applyDocumentFields(
      Map<String, Object> params, DocumentEntity document, String lang) {
    params.put("number", numberWithN(document.getNumber()));
    params.put("price", bd(toSom(document.getPrice())));
    if (document.getCurrency() != null) {
      params.put("currency", document.getCurrency().name());
    }
    putPriceInWords(params, toSom(document.getPrice()), document.getCurrency(), lang);
    params.put("initialPayment", bd(toSom(document.getInitialPayment())));
    putInitialPaymentInWords(
        params, toSom(document.getInitialPayment()), document.getCurrency(), lang);
    params.put("createdDate", fmt(document.getCreatedDate(), ZoneId.of("UTC+5")));
  }

  // Imzo bloki belgisi (badge): har bir taraf turiga qarab haqiqiy tasdiqlash
  // usulini ("SMS bilan tasdiqlangan" / "MyID..." / "E-IMZO...") qo'yadi.
  // Shablon sotib oluvchi/sotuvchi uchun jismoniy(individual) yoki yuridik(legal)
  // verifikatsiya turini saqlaydi; statik "E-IMZO/MyID" o'rniga shu ishlatiladi.
  // Imzo badge'i FAQAT haqiqatan imzolagan (ACCEPTED) tarafga qo'yiladi; aks holda
  // bo'sh — imzolanmagan shartnoma "tasdiqlangan" deb ko'rsatilmasligi uchun.
  static Map<String, Object> addVerificationLabels(
      Map<String, Object> params,
      TemplateEntity template,
      boolean sellerSigned,
      boolean buyerSigned,
      String lang) {
    applyVerifLabel(params.get("seller"), template, sellerSigned, lang);
    applyVerifLabel(params.get("buyer"), template, buyerSigned, lang);
    return params;
  }

  // Bitta taraf Map'iga "verificationLabel" qo'yadi: COMPANY -> legal, aks holda individual.
  // Imzolanmagan (signed=false) bo'lsa bo'sh — "tasdiqlangan" badge ko'rsatilmaydi.
  @SuppressWarnings("unchecked")
  private static void applyVerifLabel(
      Object partyObj, TemplateEntity template, boolean signed, String lang) {
    if (!(partyObj instanceof Map)) {
      return;
    }
    Map<String, Object> party = (Map<String, Object>) partyObj;
    if (!signed) {
      party.put("verificationLabel", "");
      return;
    }
    Object type = party.get("type");
    boolean isLegal = type != null && "COMPANY".equals(type.toString());
    VerificationType vt =
        isLegal ? template.getLegalVerificationType() : template.getIndividualVerificationType();
    party.put("verificationLabel", verifLabel(vt, lang));
  }

  // Tasdiqlash usuli -> ko'rsatiladigan matn (badge).
  private static String verifLabel(VerificationType vt, String lang) {
    if (vt == null) {
      return "";
    }
    return switch (vt) {
      case NONE -> tr(lang, "Tasdiqlangan", "Подтверждено", "Verified");
      case OTP_SMS ->
          tr(lang, "SMS bilan tasdiqlangan", "Подтверждено по SMS", "Verified via SMS");
      case MY_ID ->
          tr(lang, "MyID bilan tasdiqlangan", "Подтверждено через MyID", "Verified via MyID");
      case ABLE_ID ->
          tr(lang, "AbleID bilan tasdiqlangan", "Подтверждено через AbleID", "Verified via AbleID");
      case E_IMZO ->
          tr(lang, "E-IMZO bilan tasdiqlangan", "Подтверждено через E-IMZO", "Verified via E-IMZO");
    };
  }

  // Boshlang'ich (oldindan) to'lov so'z bilan — nasiya/oldi-sotdi shablonlari uchun.
  // Raqam ham, valyuta so'zi ham hujjat tilida (uz/ru/en).
  private static void putInitialPaymentInWords(
      Map<String, Object> params, Double amount, Currency currency, String lang) {
    long whole = amount == null ? 0L : (long) Math.floor(amount);
    String words = NumberToWords.toWords(whole, lang);
    String unit = currencyWord(currency, lang);
    params.put("initialPaymentWords", unit.isBlank() ? words : words + " " + unit);
  }

  // Summani so'z bilan (hujjat tilida) + valyuta birligi: "o'n million so'm" / "десять миллионов
  // сум" / "ten million soum".
  private static void putPriceInWords(
      Map<String, Object> params, Double price, Currency currency, String lang) {
    long whole = price == null ? 0L : (long) Math.floor(price);
    String words = NumberToWords.toWords(whole, lang);
    String unit = currencyWord(currency, lang);
    String full = unit.isBlank() ? words : words + " " + unit;
    params.put("priceInWords", full);
    params.put("priceWords", full);
    params.put("priceText", full);
    params.put("priceWord", full);
  }

  // Preview uchun to'lov jadvali qatorlari (request payments'idan).
  // buildPaymentScheduleRows bilan bir xil maydonlar — to'langan qismlarsiz.
  private static List<Map<String, Object>> previewScheduleRows(
      List<uz.hesap.service.document.model.request.DocumentRequest.PaymentItem> payments,
      Currency currency,
      String lang) {
    java.time.ZoneId zone = ZoneId.of("UTC+5");
    List<Map<String, Object>> rows = new ArrayList<>();
    int i = 0;
    for (var p : payments) {
      if (p == null || p.amount() == null) {
        continue;
      }
      i++;
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("no", String.valueOf(i));
      m.put("toPay", bd(toSom(p.amount())) + " " + currencyWord(currency, lang));
      m.put("paidPart", "");
      m.put("returnTime", p.paymentDate() != null ? fmt(p.paymentDate(), zone) : "");
      m.put("changeDate", "");
      m.put("status", statusLabel("PENDING", lang));
      m.put("paidTime", "");
      rows.add(m);
    }
    return rows;
  }

  private static String currencyWord(Currency currency, String lang) {
    if (currency == null) {
      return tr(lang, "so'm", "сум", "soum");
    }
    return switch (currency) {
      case UZS -> tr(lang, "so'm", "сум", "soum");
      case USD -> tr(lang, "AQSH dollari", "долларов США", "US dollars");
      case RUB -> tr(lang, "rubl", "рублей", "rubles");
    };
  }

  // Party'ni Map'ga aylantirish — Jasper template'da $P{seller}.get("...") sifatida
  // chaqiriladi. Barcha qiymatlar String (null'lar bo'sh string'ga aylantiriladi).
  // type/isVerified — String shaklida (Jasper toString() chaqirmasligi uchun).
  private static Map<String, String> toPartyMap(UserPassportBasicResponse party) {
    Map<String, String> m = new LinkedHashMap<>();
    m.put("fullName", nullToEmpty(party.fullName()));
    m.put("firstName", nullToEmpty(party.firstName()));
    m.put("lastName", nullToEmpty(party.lastName()));
    m.put("midName", nullToEmpty(party.midName()));
    m.put("phone", nullToEmpty(party.phone()));
    m.put("pinfl", nullToEmpty(party.in()));
    // `in` — universal identifikator alias (jismoniyda PINFL, yuridikda TIN).
    // Template: $P{seller}.get("in") ham ishlaydi.
    m.put("in", nullToEmpty(party.in()));
    m.put("passport", nullToEmpty(party.document()));
    m.put("document", nullToEmpty(party.document())); // passport alias
    m.put("legalName", nullToEmpty(party.legalName()));
    m.put("tin", nullToEmpty(party.tin()));
    // COMPANY (yuridik shaxs) tomonining direktori (OWNER staff) F.I.SH — jismoniyda bo'sh.
    m.put("ownerFullName", nullToEmpty(party.ownerFullName()));
    m.put("ownerFirstName", nullToEmpty(party.ownerFirstName()));
    m.put("ownerLastName", nullToEmpty(party.ownerLastName()));
    m.put("ownerMidName", nullToEmpty(party.ownerMidName()));
    m.put("address", nullToEmpty(party.address()));
    m.put("type", party.type() != null ? party.type().name() : "");
    m.put("isVerified", String.valueOf(Boolean.TRUE.equals(party.isVerified())));
    return m;
  }

  // To'liq nom: yuridik shaxs (COMPANY) -> legalName, jismoniy -> F.I.O.
  private static String buildFullName(uz.hesap.service.common.util.UserResponse user) {
    if (user.type() == uz.hesap.service.common.util.enums.UserType.COMPANY
        && user.legalName() != null
        && !user.legalName().isBlank()) {
      return user.legalName();
    }
    return java.util.stream.Stream.of(user.lastName(), user.firstName(), user.midName())
        .filter(part -> part != null && !part.isBlank())
        .collect(Collectors.joining(" "));
  }

  // Shartnoma mahsulotlari -> $P{products} (JRMapCollectionDataSource uchun List<Map>).
  // Maydonlar JrxmlBuilder defaultProductColumns bilan bir xil: productName/Unit/Amount/Price/Total
  // + productStatus/productPaid (mahsulot so'rovlaridan hisoblangan holat va berilgan miqdor).
  public static List<Map<String, Object>> buildProductRows(
      List<ContractProductEntity> products,
      List<ProductRequestEntity> productRequests,
      boolean isMoneyMode,
      String lang) {
    List<Map<String, Object>> rows = new ArrayList<>();
    if (products == null) return rows;
    for (ContractProductEntity p : products) {
      if (p == null) continue;
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("productName", nullToEmpty(p.getName()));
      m.put("productUnit", unitLabel(p.getUnit(), lang));
      m.put("productAmount", p.getQuantity() == null ? "" : trimNumber(p.getQuantity()));
      m.put("productPrice", p.getPrice() == null ? "" : formatMoney(toSom(p.getPrice())));
      m.put("productTotal", p.getAmount() == null ? "" : formatMoney(toSom(p.getAmount())));
      ProductProgress progress =
          computeProductProgress(
              productRequests, p.getId(), isMoneyMode, p.getQuantity(), p.getAmount(), lang);
      m.put("productPaid", formatProductGiven(progress.given(), isMoneyMode));
      m.put("productStatus", progress.statusLabel());
      rows.add(m);
    }
    return rows;
  }

  // Preview so'rovidagi mahsulotlar -> $P{products} (buildProductRows bilan bir xil
  // maydonlar, faqat entity o'rniga request DTO'dan — hujjat hali saqlanmagan, shuning
  // uchun so'rovlar/holat mavjud emas: har doim "Berilmagan" + 0).
  public static List<Map<String, Object>> buildPreviewProductRows(
      List<ContractProductRequest> products, String lang) {
    List<Map<String, Object>> rows = new ArrayList<>();
    if (products == null) return rows;
    for (ContractProductRequest p : products) {
      if (p == null) continue;
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("productName", nullToEmpty(p.name()));
      m.put("productUnit", unitLabel(p.unit(), lang));
      m.put("productAmount", p.quantity() == null ? "" : trimNumber(p.quantity()));
      m.put("productPrice", p.price() == null ? "" : formatMoney(toSom(p.price())));
      m.put("productTotal", p.amount() == null ? "" : formatMoney(toSom(p.amount())));
      m.put("productPaid", formatProductGiven(0, false));
      m.put("productStatus", tr(lang, "Berilmagan", "Не передано", "Not delivered"));
      rows.add(m);
    }
    return rows;
  }

  // Mahsulot bo'yicha APPROVED so'rovlardan hisoblangan berilgan miqdor + holat matni —
  // iOS ContractDetailViewModel.productGiven/productStatus bilan bir xil mantiq.
  private record ProductProgress(double given, String statusLabel) {}

  private static ProductProgress computeProductProgress(
      List<ProductRequestEntity> requests,
      UUID productId,
      boolean isMoneyMode,
      Double productQuantity,
      Double productAmount,
      String lang) {
    List<ProductRequestEntity> forProduct =
        (requests == null ? List.<ProductRequestEntity>of() : requests)
            .stream()
                .filter(r -> r != null && productId != null && productId.equals(r.getProductId()))
                .toList();
    double given =
        forProduct.stream()
            .filter(r -> r.getStatus() == ProductRequestStatus.APPROVED)
            .mapToDouble(
                r ->
                    isMoneyMode
                        ? (r.getAmount() != null ? r.getAmount() : nz(productAmount))
                        : (r.getQuantity() != null ? r.getQuantity() : nz(productQuantity)))
            .sum();
    double total = isMoneyMode ? nz(productAmount) : nz(productQuantity);
    String status;
    if (total > 0 && given >= total) {
      status = tr(lang, "Berilgan", "Передано", "Delivered");
    } else if (given > 0) {
      status = tr(lang, "Qisman berilgan", "Частично передано", "Partially delivered");
    } else if (forProduct.stream().anyMatch(r -> r.getStatus() == ProductRequestStatus.PENDING)) {
      status = tr(lang, "Kutilmoqda", "Ожидается", "Pending");
    } else {
      status = tr(lang, "Berilmagan", "Не передано", "Not delivered");
    }
    return new ProductProgress(given, status);
  }

  private static double nz(Double d) {
    return d == null ? 0.0 : d;
  }

  // MONEY — so'm (tiyin/100), GOODS — sof son (soni).
  private static String formatProductGiven(double given, boolean isMoneyMode) {
    return isMoneyMode ? formatMoney(toSom(given)) : trimNumber(given);
  }

  private static String unitLabel(ProductUnit u, String lang) {
    if (u == null) return "";
    return switch (u) {
      case DONA -> tr(lang, "dona", "шт", "pcs");
      case KG -> tr(lang, "kg", "кг", "kg");
      case LITR -> tr(lang, "litr", "литр", "liter");
    };
  }

  private static String trimNumber(Double d) {
    if (d == null) return "";
    return d == Math.floor(d) ? String.valueOf(d.longValue()) : String.valueOf(d);
  }

  private static String nullToEmpty(String value) {
    return value == null ? "" : value;
  }

  private static boolean isBlank(Object value) {
    return value == null || (value instanceof String s && s.isBlank());
  }

  private static String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value;
      }
    }
    return "";
  }

  // Saqlanmagan preview: shablon + to'ldirilgan qiymatlardan PDF render qiladi.
  // Saqlangan hujjat oqimi (generate) bilan bir xil param tuzilishi ishlatiladi.
  public Mono<byte[]> preview(final DocumentPreviewRequest request, final String creatorIn) {
    return templateFieldRepository
        .findAllByTemplateIdAndDeletedFalse(request.templateId())
        .collectList()
        .flatMap(
            fields ->
                customDocumentRepository
                    .peekDailyNumber(java.time.LocalDate.now())
                    .flatMap(nextNo -> renderPreview(request, fields, nextNo, creatorIn)));
  }

  // Preview render: prospektiv raqam (YYMMDD-NNNN) bilan, request.number() e'tiborsiz.
  private Mono<byte[]> renderPreview(
      final DocumentPreviewRequest request,
      final List<TemplateFieldEntity> fields,
      final int nextNo,
      final String creatorIn) {
    final String lang = request.lang();
    Map<String, Object> params = toPreviewMap(request, fields);
    // Hali saqlanmagan hujjat uchun taxminiy raqam — frontend "001" yubormaydi.
    params.put("number", previewNumber(nextNo));
      // Kiritilgan summa/valyuta — dummy o'rniga.
      if (request.price() != null) {
        params.put("price", bd(toSom(request.price())));
      }
              if (request.currency() != null) {
                params.put("currency", request.currency().name());
              }
              Double effPriceTiyin = request.price() != null ? request.price() : 10_000_000.0;
              Currency effCur = request.currency() != null ? request.currency() : Currency.UZS;
              putPriceInWords(params, toSom(effPriceTiyin), effCur, lang);
              // To'lov jadvali (berilgan bo'lsa) — dummy bo'sh ro'yxat o'rniga.
              if (request.payments() != null && !request.payments().isEmpty()) {
                params.put(
                    "paymentScheduleList", previewScheduleRows(request.payments(), effCur, lang));
              }
              return applyPreviewParties(request, params, creatorIn)
                  .flatMap(
                      enriched ->
                          templateRepository
                              .findById(request.templateId())
                              .switchIfEmpty(
                                  Mono.error(new NotFoundException("Template not found")))
                              .flatMap(
                                  // Saqlangan shartnoma bilan AYNAN bir xil render yo'li
                                  // (renderWithTemplate). Preview — imzolar hali yo'q (false/false).
                                  template -> renderWithTemplate(template, enriched, false, false, lang)));
  }

  // Preview uchun prospektiv raqamni YYMMDD-NNNN formatida qaytaradi.
  private static String previewNumber(final int no) {
    return java.time.LocalDate.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("yyMMdd"))
        + "-"
        + String.format("%04d", no);
  }

  // buyer/seller in (PINFL/STIR) berilgan bo'lsa, dummy o'rniga DB'dan haqiqiy taraf
  // ma'lumotini (ism, JSHSHIR, telefon...) oladi. Yuboruvchi tomon bo'sh bo'lsa
  // creatorIn (principal) bilan to'ldiriladi (create bilan bir xil).
  private Mono<Map<String, Object>> applyPreviewParties(
      DocumentPreviewRequest request, Map<String, Object> params, String creatorIn) {
    final String buyerIn =
        (request.buyerIn() != null && !request.buyerIn().isBlank())
            ? request.buyerIn()
            : creatorIn;
    final String sellerIn =
        (request.sellerIn() != null && !request.sellerIn().isBlank())
            ? request.sellerIn()
            : creatorIn;
    if ((sellerIn == null || sellerIn.isBlank()) && (buyerIn == null || buyerIn.isBlank())) {
      return Mono.just(params);
    }
    Mono<UserPassportBasicResponse> seller =
        fetchPartyByIn(sellerIn).defaultIfEmpty(emptyParty());
    Mono<UserPassportBasicResponse> buyer = fetchPartyByIn(buyerIn).defaultIfEmpty(emptyParty());
    return Mono.zip(seller, buyer)
        .map(
            tuple -> {
              if (sellerIn != null && !sellerIn.isBlank()) {
                params.put("seller", toPartyMap(tuple.getT1()));
              }
              if (buyerIn != null && !buyerIn.isBlank()) {
                params.put("buyer", toPartyMap(tuple.getT2()));
              }
              return params;
            });
  }

  // toMap bilan bir xil mantiq, lekin saqlangan DocumentValue o'rniga request
  // qiymatlaridan. Payment schedule preview'da yo'q — bo'sh ro'yxat beriladi.
  public static Map<String, Object> toPreviewMap(
      final DocumentPreviewRequest request, final List<TemplateFieldEntity> templateFields) {
    Map<UUID, TemplateFieldEntity> fieldById =
        templateFields.stream()
            .filter(tf -> tf.getId() != null)
            .collect(
                Collectors.toMap(TemplateFieldEntity::getId, Function.identity(), (a, b) -> a));

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("number", request.number() != null ? request.number() : "");

    Map<String, Map<Integer, Map<String, Object>>> groupedRows = new LinkedHashMap<>();

    List<DocumentPreviewRequest.PreviewValue> values =
        request.values() == null ? List.of() : request.values();

    for (DocumentPreviewRequest.PreviewValue dv : values) {
      if (dv.templateFieldId() == null) continue;

      TemplateFieldEntity tf = fieldById.get(dv.templateFieldId());
      if (tf == null) continue;
      if (Boolean.TRUE.equals(tf.getDeleted())) continue;

      String keyName = tf.getKeyName();
      if (keyName == null || keyName.isBlank()) continue;

      Object value = dv.value();
      String parentKey = tf.getParentKey();

      if (parentKey == null || parentKey.isBlank()) {
        result.put(keyName, value);
        continue;
      }

      Integer pos = dv.position();
      if (pos == null) continue;

      Map<Integer, Map<String, Object>> rowsByPos =
          groupedRows.computeIfAbsent(parentKey, k -> new TreeMap<>());

      Map<String, Object> row =
          rowsByPos.computeIfAbsent(
              pos,
              p -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("position", p);
                return m;
              });

      row.put(keyName, value);
    }

    for (Map.Entry<String, Map<Integer, Map<String, Object>>> e : groupedRows.entrySet()) {
      result.put(e.getKey(), new ArrayList<>(e.getValue().values()));
    }

    // Preview'da saqlangan to'lov jadvali bo'lmaydi.
    result.put("paymentScheduleList", new ArrayList<>());

    // Preview'da saqlangan mahsulot yo'q — request.products()'dan quriladi.
    result.put("products", buildPreviewProductRows(request.products(), request.lang()));

    // Preview'da imzolagan guvohlar bo'lmaydi.
    result.put("witnessRows", new ArrayList<>());

    // Preview uchun static hujjat maydonlari — dummy seller/buyer'dek, admin
    // template'ni real ko'rinishda ko'ra olsin. Saqlangan hujjat oqimida
    // (generate) applyDocumentFields haqiqiy qiymatlarni DocumentEntity'dan oladi.
    // request'dan kelgan qiymat (values orqali) ustivor — shu sababli containsKey.
    if (!result.containsKey("price")) {
      result.put("price", bd(10_000_000.0));
    }
    if (!result.containsKey("currency")) {
      result.put("currency", "UZS");
    }
    if (!result.containsKey("createdDate")) {
      result.put("createdDate", fmt(Instant.now(), ZoneId.of("UTC+5")));
    }
    if (!result.containsKey("initialPayment")) {
      result.put("initialPayment", bd(2_000_000.0));
      result.put(
          "initialPaymentWords",
          NumberToWords.toWords(2_000_000L, request.lang())
              + " "
              + currencyWord(Currency.UZS, request.lang()));
    }

    // Preview uchun dummy seller/buyer — admin template'ni real ko'rinishda
    // test qila olsin. Saqlangan hujjat oqimida (generate) enrichParams
    // haqiqiy user ma'lumotlarini DB'dan oladi.
    if (!result.containsKey("seller")) {
      result.put("seller", dummyParty("Hakimov Is'hoq Ismat o'g'li", "AB1234567", "12345678901234"));
    }
    if (!result.containsKey("buyer")) {
      result.put("buyer", dummyParty("Karimov Ali Vali o'g'li", "CD7654321", "43210987654321"));
    }

    return result;
  }

  private static Map<String, String> dummyParty(String fullName, String passport, String pinfl) {
    Map<String, String> m = new LinkedHashMap<>();
    String[] parts = fullName.split(" ", 3);
    m.put("fullName", fullName);
    m.put("firstName", parts.length > 0 ? parts[0] : "");
    m.put("lastName", parts.length > 1 ? parts[1] : "");
    m.put("midName", parts.length > 2 ? parts[2] : "");
    m.put("phone", "+998 90 000 00 00");
    m.put("pinfl", pinfl);
    m.put("passport", passport);
    m.put("document", passport);
    m.put("legalName", "");
    m.put("tin", "");
    m.put("address", "Toshkent shahri");
    m.put("type", "CLIENT");
    m.put("isVerified", "true");
    m.put("verificationLabel", "SMS bilan tasdiqlangan");
    return m;
  }

  public static Map<String, Object> toMap(
      final DocumentEntity document,
      final List<DocumentValueEntity> documentValues,
      final List<TemplateFieldEntity> templateFields,
      List<PaymentEntity> schedules,
      List<PaidScheduleEntity> paidParts,
      List<ContractProductEntity> products,
      List<ProductRequestEntity> productRequests,
      boolean isMoneyMode,
      String lang) {
    Map<UUID, TemplateFieldEntity> fieldById =
        templateFields.stream()
            .filter(tf -> tf.getId() != null)
            .collect(
                Collectors.toMap(TemplateFieldEntity::getId, Function.identity(), (a, b) -> a));

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("number", document.getNumber());

    Map<String, Map<Integer, Map<String, Object>>> groupedRows = new LinkedHashMap<>();

    for (DocumentValueEntity dv : documentValues) {
      if (Boolean.TRUE.equals(dv.getDeleted())) continue;
      if (dv.getTemplateFieldId() == null) continue;

      TemplateFieldEntity tf = fieldById.get(dv.getTemplateFieldId());
      if (tf == null) continue;
      if (Boolean.TRUE.equals(tf.getDeleted())) continue;

      String keyName = tf.getKeyName();
      if (keyName == null || keyName.isBlank()) continue;

      Object value = dv.getValue();
      String parentKey = tf.getParentKey();

      if (parentKey == null || parentKey.isBlank()) {
        result.put(keyName, value);
        continue;
      }

      Integer pos = dv.getPosition();
      if (pos == null) continue;

      Map<Integer, Map<String, Object>> rowsByPos =
          groupedRows.computeIfAbsent(parentKey, k -> new TreeMap<>());

      Map<String, Object> row =
          rowsByPos.computeIfAbsent(
              pos,
              p -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("position", p);
                return m;
              });

      row.put(keyName, value);
    }

    for (Map.Entry<String, Map<Integer, Map<String, Object>>> e : groupedRows.entrySet()) {
      result.put(e.getKey(), new ArrayList<>(e.getValue().values()));
    }

    result.put(
        "paymentScheduleList",
        buildPaymentScheduleRows(
            schedules, paidParts, ZoneId.of("UTC+5"), document.getCurrency(), lang));

    result.put("products", buildProductRows(products, productRequests, isMoneyMode, lang));

    return result;
  }

  // Imzolagan (ACCEPTED) guvohlar — PDF "witnessRows" jadvali uchun rekvizitlar
  // (F.I.Sh., PINFL, telefon, imzo sanasi). Guvoh bo'lmasa bo'sh ro'yxat.
  private Mono<List<Map<String, Object>>> buildWitnessRows(
      List<uz.hesap.service.document.domain.document.WitnessRequestEntity> requests) {
    List<uz.hesap.service.document.domain.document.WitnessRequestEntity> accepted =
        requests == null
            ? List.of()
            : requests.stream()
                .filter(
                    w ->
                        w.getStatus()
                            == uz.hesap.service.document.domain.enums.DocumentWitnessStatus
                                .ACCEPTED)
                .toList();
    if (accepted.isEmpty()) {
      return Mono.just(List.of());
    }
    return userServiceClient
        .getUsersByIds(
            accepted.stream()
                .map(uz.hesap.service.document.domain.document.WitnessRequestEntity::getWitnessId)
                .toList())
        .collectMap(uz.hesap.service.common.util.UserResponse::id, u -> u)
        .map(
            users ->
                accepted.stream()
                    .map(
                        w -> {
                          var u = users.get(w.getWitnessId());
                          Map<String, Object> row = new LinkedHashMap<String, Object>();
                          String fullName =
                              u == null
                                  ? ""
                                  : String.join(
                                          " ",
                                          nullToEmpty(u.lastName()),
                                          nullToEmpty(u.firstName()),
                                          nullToEmpty(u.midName()))
                                      .trim()
                                      .replaceAll("\\s+", " ");
                          row.put("witnessFullName", fullName);
                          row.put("witnessPinfl", u == null ? "" : nullToEmpty(u.in()));
                          row.put("witnessPhone", u == null ? "" : nullToEmpty(u.phone()));
                          row.put(
                              "witnessSignedDate",
                              fmt(w.getLastModifiedDate(), ZoneId.of("UTC+5")));
                          return (Map<String, Object>) row;
                        })
                    .toList())
        .onErrorReturn(List.of());
  }

  private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");

  private static String fmt(Instant t, ZoneId zone) {
    if (t == null) return "";
    return DATE.format(t.atZone(zone));
  }

  private static String bd(Double v) {
    DecimalFormatSymbols s = new DecimalFormatSymbols();
    s.setGroupingSeparator(' ');
    s.setDecimalSeparator('.');

    DecimalFormat df = new DecimalFormat("###,###,###,###,##0.00", s);
    return v == null ? "" : df.format(v);
  }

  // Barcha pul summalari bazada/requestda TIYIN'da saqlanadi (so'm x 100) — PDF'da
  // ko'rsatishdan oldin so'mga aylantiriladi, aks holda 100x katta ko'rsatiladi.
  private static Double toSom(Double tiyin) {
    return tiyin == null ? null : tiyin / 100.0;
  }

  public static List<Map<String, Object>> buildPaymentScheduleRows(
      List<PaymentEntity> schedules,
      List<PaidScheduleEntity> paidParts,
      ZoneId zoneId,
      Currency currency,
      String lang) {
    List<PaymentEntity> sch =
        schedules == null
            ? List.of()
            : schedules.stream()
                .filter(s -> !Boolean.TRUE.equals(s.getDeleted()))
                .sorted(
                    Comparator.comparing(
                        PaymentEntity::getContractPaymentDate,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

    Map<UUID, List<PaidScheduleEntity>> paidByScheduleId =
        (paidParts == null ? List.<PaidScheduleEntity>of() : paidParts)
            .stream()
                .filter(p -> !Boolean.TRUE.equals(p.getDeleted()))
                .filter(p -> p.getPaymentScheduleId() != null)
                .collect(
                    Collectors.groupingBy(
                        PaidScheduleEntity::getPaymentScheduleId,
                        LinkedHashMap::new,
                        Collectors.collectingAndThen(
                            Collectors.toList(),
                            list -> {
                              list.sort(
                                  Comparator.comparing(
                                      PaidScheduleEntity::getPaymentDate,
                                      Comparator.nullsLast(Comparator.naturalOrder())));
                              return list;
                            })));

    List<Map<String, Object>> rows =
        new ArrayList<>(sch.size() + (paidParts == null ? 0 : paidParts.size()));

    int i = 0;
    for (PaymentEntity s : sch) {
      i++;

      List<PaidScheduleEntity> parts = paidByScheduleId.getOrDefault(s.getId(), List.of());

      // Jami to'langan summa: entity.paidAmount (kumulyativ) yoki paid qismlar
      // yig'indisi (paidAmount bo'sh qolgan eski ma'lumot uchun) — kattasini olamiz.
      double paidCum = s.getPaidAmount() != null ? s.getPaidAmount() : 0.0;
      double partsSum =
          parts.stream().mapToDouble(p -> p.getAmount() != null ? p.getAmount() : 0.0).sum();
      double paid = Math.max(paidCum, partsSum);
      double total = s.getTotalAmount() != null ? s.getTotalAmount() : 0.0;

      // Main row "1"
      Map<String, Object> main = new LinkedHashMap<>();
      main.put("no", String.valueOf(i));
      main.put(
          "toPay", bd(toSom(s.getTotalAmount())) + " " + currencyWord(currency, lang)); // "К оплате"
      // "To'langan" ustunida jami to'langan summa (qisman bo'lsa ham ko'rinadi).
      main.put(
          "paidPart",
          paid > 0 ? bd(toSom(paid)) + " " + currencyWord(currency, lang) : "");
      main.put("returnTime", fmt(s.getContractPaymentDate(), zoneId)); // "Время возврата"
      main.put("changeDate", fmt(s.getUpdatedAt(), zoneId)); // "Дата изменения"
      // Holat: to'liq→To'langan, qisman(0<paid<total)→Qisman to'langan, aks holda status.
      main.put(
          "status",
          paymentStatusLabel(s.getStatus() != null ? s.getStatus().name() : "", paid, total, lang));
      main.put("paidTime", "");
      rows.add(main);

      // Qism-qatorlar (har bir to'lov yozuvi) OLIB TASHLANDI: ular paid_schedule'ning
      // ichki tasdiqlash holatini (PENDING) ko'rsatib, to'liq to'langan installmentda
      // ham "Kutilmoqda" chiqib chalg'itardi. Asosiy qatorning o'zi jami to'langan
      // summa + to'g'ri holatni (To'langan / Qisman to'langan / ...) ko'rsatadi.
    }

    return rows;
  }

  // To'lov jadvali asosiy qatori holati — to'langan summani hisobga oladi:
  //  - CANCELLED → Bekor qilingan
  //  - to'liq (status=PAID yoki paid>=total) → To'langan
  //  - qisman (0 < paid < total) → Qisman to'langan
  //  - aks holda entity status (PENDING → Kutilmoqda, ...).
  private static String paymentStatusLabel(String status, double paid, double total, String lang) {
    if ("CANCELLED".equals(status)) {
      return statusLabel("CANCELLED", lang);
    }
    if ("PAID".equals(status) || (total > 0 && paid >= total)) {
      return statusLabel("PAID", lang);
    }
    if (paid > 0) {
      return tr(lang, "Qisman to'langan", "Частично оплачено", "Partially paid");
    }
    return statusLabel(status, lang);
  }

  // To'lov holati — PDF ko'rish tilida (PENDING -> "Kutilmoqda"/"Ожидается"/"Pending" ...).
  private static String statusLabel(String status, String lang) {
    if (status == null || status.isBlank()) {
      return "";
    }
    return switch (status) {
      case "PENDING" -> tr(lang, "Kutilmoqda", "Ожидается", "Pending");
      case "PAID" -> tr(lang, "To'langan", "Оплачено", "Paid");
      case "APPROVED" -> tr(lang, "Tasdiqlangan", "Подтверждено", "Approved");
      case "CANCELLED" -> tr(lang, "Bekor qilingan", "Отменено", "Cancelled");
      default -> status;
    };
  }

  // Til bo'yicha matn tanlaydi (uz/ru/en). null/noma'lum → uz.
  private static String tr(String lang, String uz, String ru, String en) {
    if (lang == null) return uz;
    return switch (lang.toLowerCase()) {
      case "ru" -> ru;
      case "en" -> en;
      default -> uz;
    };
  }
}
