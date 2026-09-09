package uz.hesap.service.integration.domain;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;
import uz.hesap.service.common.util.enums.PaymentMethod;
import uz.hesap.service.integration.domain.enums.PurchaseType;

// Xaridlar jurnali — tarif/paket sotib olinganda yoziladi.
@Getter
@Setter
@Table(schema = "\"user\"", name = "purchases")
public class PurchaseEntity {
  @Id private UUID id;
  private Double amount;
  private String userIn; // foydalanuvchi PINFL/STIR (userId o'rniga)
  private String promo; // qo'llangan promokod (bo'lsa)
  private PaymentMethod paymentMethod;
  // unitType — nima sotib olingani (PurchaseType: TARIFF/PACKAGE).
  private PurchaseType unitType;
  private UUID unitId;
  private UUID createdBy;
  private UUID updatedBy;
  @CreatedDate private Instant createdAt;
  @LastModifiedDate private Instant updatedAt;
}
