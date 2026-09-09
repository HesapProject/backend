package uz.hesap.service.document.service.c2c;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.hesap.service.document.domain.document.ContractProductEntity;
import uz.hesap.service.document.domain.document.DocumentEntity;
import uz.hesap.service.document.domain.document.ProductRequestEntity;
import uz.hesap.service.document.domain.enums.RoumingType;
import uz.hesap.service.document.repository.ContractProductRepository;
import uz.hesap.service.document.repository.TemplateRepository;
import uz.hesap.service.document.webclient.IntegrationServiceClient;

// Oldi-berdi tasdiqlanganda Rouming (Factura Provider)'da draft schyot-faktura
// yaratish. Ma'lumot shartnoma + mahsulotdan yig'iladi va integration servisiga
// s2s yuboriladi. Bu side-effect — xatolik asosiy oqimni to'xtatmasligi kerak
// (chaqiruvchi onErrorResume bilan qamraydi).
@Slf4j
@Service
@RequiredArgsConstructor
public class RoumingFacturaService {

  private static final DateTimeFormatter DATE =
      DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.of("Asia/Tashkent"));

  private final ContractProductRepository contractProductRepository;
  private final TemplateRepository templateRepository;
  private final IntegrationServiceClient integrationServiceClient;

  // APPROVED bo'lgan so'rov uchun draft ЭСФ. Summalar tiyin'dan so'mga (÷100).
  // Shablon roumingType bo'yicha: NULL -> yuborilmaydi; FACTURA -> draft ЭСФ;
  // qolgan turlar uchun provider endpointi hali yo'q — log bilan o'tkaziladi.
  public Mono<Void> sendDraftForApprovedRequest(ProductRequestEntity pr, DocumentEntity doc) {
    if (doc.getTemplateId() == null) {
      return Mono.empty();
    }
    return templateRepository
        .findById(doc.getTemplateId())
        .flatMap(
            template -> {
              RoumingType type = template.getRoumingType();
              if (type == null || type == RoumingType.NONE) {
                return Mono.<Void>empty();
              }
              if (type != RoumingType.FACTURA) {
                log.info(
                    "Rouming type {} not implemented yet, contract {} skipped",
                    type,
                    doc.getNumber());
                return Mono.<Void>empty();
              }
              return sendFacturaDraft(pr, doc);
            });
  }

  private Mono<Void> sendFacturaDraft(ProductRequestEntity pr, DocumentEntity doc) {
    Mono<ContractProductEntity> productMono =
        pr.getProductId() == null
            ? Mono.empty()
            : contractProductRepository.findById(pr.getProductId());
    return productMono
        .map(p -> buildRequest(pr, doc, p))
        .defaultIfEmpty(buildRequest(pr, doc, null))
        .flatMap(integrationServiceClient::createRoumingFacturaDraft)
        .doOnSuccess(r -> log.info("Rouming draft created for contract {}", doc.getNumber()))
        .then();
  }

  private IntegrationServiceClient.RoumingFacturaDraftRequest buildRequest(
      ProductRequestEntity pr, DocumentEntity doc, ContractProductEntity product) {
    // Soni: so'rovda ko'rsatilgan (qisman) yoki mahsulotning to'liq soni.
    Double count =
        pr.getQuantity() != null
            ? pr.getQuantity()
            : product != null && product.getQuantity() != null ? product.getQuantity() : 1d;
    // Birlik narxi so'mda (tiyin ÷ 100).
    Double unitPrice =
        product != null && product.getPrice() != null ? product.getPrice() / 100d : 0d;
    // Jami: so'rov summasi (tiyin) bo'lsa undan, aks holda narx × soni.
    Double totalSum =
        pr.getAmount() != null && pr.getAmount() > 0
            ? pr.getAmount() / 100d
            : unitPrice * count;
    String name =
        product != null && product.getName() != null && !product.getName().isBlank()
            ? product.getName()
            : "Mahsulot";

    String today = DATE.format(Instant.now());
    String contractDate =
        doc.getCreatedDate() != null ? DATE.format(doc.getCreatedDate()) : today;
    // ЭСФ raqami unikal bo'lishi uchun shartnoma raqami + so'rov id qisqartmasi.
    String facturaNo =
        doc.getNumber() + "-" + pr.getId().toString().substring(0, 8).toUpperCase();

    return new IntegrationServiceClient.RoumingFacturaDraftRequest(
        doc.getId(),
        doc.getSellerIn(),
        doc.getBuyerIn(),
        null,
        null,
        facturaNo,
        today,
        doc.getNumber(),
        contractDate,
        List.of(
            new IntegrationServiceClient.RoumingProductLine(name, count, unitPrice, totalSum)));
  }
}
