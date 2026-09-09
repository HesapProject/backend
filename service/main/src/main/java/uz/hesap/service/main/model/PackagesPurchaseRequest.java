package uz.hesap.service.main.model;

import java.util.UUID;
import org.springframework.util.Assert;

// Paket sotib olish so'rovi. userId — faqat admin/super_admin uchun (kimga sotib olinadi).
// promoCode — ixtiyoriy; berilsa server tekshirib chegirmali narxni hisoblaydi.
public record PackagesPurchaseRequest(UUID packageId, UUID userId, String promoCode) {
  public PackagesPurchaseRequest {
    Assert.notNull(packageId, "packageId is required");
  }
}
