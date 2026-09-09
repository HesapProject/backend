package uz.hesap.service.document.model.response;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import uz.hesap.service.document.domain.enums.ProductUnit;

public record ContractProductResponse(
    UUID id,
    String name,
    ProductUnit unit,
    Double price,
    Double quantity,
    Double amount,
    Instant deliveryAt,
    Map<String, Object> values) {}
