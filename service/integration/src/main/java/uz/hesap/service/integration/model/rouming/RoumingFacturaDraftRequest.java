package uz.hesap.service.integration.model.rouming;

import java.util.List;
import java.util.UUID;

// Oldi-berdi tasdiqlanganda document servisi yuboradigan draft ЭСФ ma'lumoti.
// Summalar SO'MDA (soliq formati) — tiyin emas. facturaDate/contractDate: yyyy-MM-dd.
public record RoumingFacturaDraftRequest(
    UUID contractId,
    String sellerTin,
    String buyerTin,
    String sellerName,
    String buyerName,
    String facturaNo,
    String facturaDate,
    String contractNo,
    String contractDate,
    List<ProductLine> products) {

  public record ProductLine(String name, Double count, Double unitPrice, Double totalSum) {}
}
