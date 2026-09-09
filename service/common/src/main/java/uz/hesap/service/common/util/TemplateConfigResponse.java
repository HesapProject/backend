package uz.hesap.service.common.util;

import java.util.List;
import uz.hesap.service.common.util.enums.TemplateDistributionType;

// tariff/paket template konfiguratsiyasi javobi
public record TemplateConfigResponse(
    TemplateDistributionType type, // SHARED yoki PER_TEMPLATE
    Integer totalCount, // SHARED da umumiy son
    List<TariffTemplateResponse> templates) {}
