package uz.hesap.service.main.model.tariff;

import java.util.List;
import uz.hesap.service.common.exception.BadRequestException;
import uz.hesap.service.common.util.enums.TemplateDistributionType;

// tariff/paket uchun template config
// SHARED: totalCount beriladi, templates — faqat templateId lari (count null)
// PER_TEMPLATE: har bir template da count beriladi
public record TariffTemplateConfig(
    TemplateDistributionType type, Integer totalCount, List<TariffTemplateRequest> templates) {

  public TariffTemplateConfig {
    // Ensure templates list is never null to avoid NPEs later
    templates = (templates == null) ? List.of() : templates;
    // type kelmasa (eski/kesh frontend) — SHARED default. switch(null) NPE bermasin.
    if (type == null) {
      type = TemplateDistributionType.SHARED;
    }

    switch (type) {
      case PER_TEMPLATE -> validatePerTemplate(templates);
      // SHARED da totalCount FAQAT shablon(lar) tanlangan bo'lsa kerak — shablonsiz
      // paketda (faqat stars/muddat, template cheklovisiz) totalCount ishlatilmaydi
      // (provisionTariff bo'sh templates'ni skip qiladi). Shu sabab shablonsiz
      // paketni totalCount'siz yaratish mumkin bo'lsin.
      case SHARED -> {
        if (!templates.isEmpty() && totalCount == null) {
          throw new BadRequestException("Total count is required for SHARED distribution");
        }
      }
    }
  }

  private void validatePerTemplate(List<TariffTemplateRequest> templates) {
    boolean missingCount = templates.stream().anyMatch(t -> t.count() == null);

    if (missingCount) {
      throw new BadRequestException("Individual counts are required for PER_TEMPLATE distribution");
    }
  }
}
