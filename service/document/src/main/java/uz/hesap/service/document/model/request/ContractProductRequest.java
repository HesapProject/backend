package uz.hesap.service.document.model.request;

import java.time.Instant;
import java.util.Map;
import uz.hesap.service.document.domain.enums.ProductUnit;

// Shartnoma mahsuloti: nom + o'lchov birligi (enum) + narx + soni
// + jami summa (amount = price*quantity) + topshirish sanasi + product field
// qiymatlari (keyName→qiymat).
public record ContractProductRequest(
    String name,
    ProductUnit unit,
    Double price,
    Double quantity,
    Double amount,
    Instant deliveryAt,
    Map<String, Object> values) {}
