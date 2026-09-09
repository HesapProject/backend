package uz.hesap.service.integration.domain;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

// To'g'ridan-to'g'ri Payme/Click paket xaridi uchun "kutilayotgan xarid" yozuvi.
// Checkout link so'ralganda PENDING yaratiladi; to'lov muvaffaqiyatli bo'lgach
// (Payme/Click webhook) shu yozuv topilib paket grant qilinadi va PAID bo'ladi.
// Balans foydalanuvchiga ko'rinmaydi — to'lov→grant oralig'ida faqat texnik hisob.
@Getter
@Setter
@ToString
@Table(schema = "integration", name = "payment_order")
public class PaymentOrderEntity {
  @Id private UUID id;
  // Checkout hisobi = to'lovchi id (C2C: user id). Webhook shu bo'yicha topadi.
  private UUID uniqueId;
  // Paketni kimga biriktirish (PINFL) — main grant uchun.
  private String userIn;
  private UUID packageId;
  // So'mda (Payme/Click checkout link ham so'mda oladi, webhook ham so'mga aylantiradi).
  private Double amount;
  private String promoCode;
  // PAYME | CLICK
  private String provider;
  // PENDING | PAID | CANCELED
  private String status;
  @CreatedDate private Instant createdDate;
  @LastModifiedDate private Instant lastModifiedDate;
  @Version private Long version;
}
