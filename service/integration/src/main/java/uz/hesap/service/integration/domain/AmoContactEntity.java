package uz.hesap.service.integration.domain;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import uz.hesap.service.integration.util.Constants;

// Hesap foydalanuvchisi ↔ amoCRM kontakti bog'lami (eski crm-app amo.contacts o'rniga).
// amoContactId — amoCRM /api/v4/contacts javobidagi kontakt id'si.
@Getter
@Setter
@ToString
@Table(schema = Constants.SCHEMA, name = Constants.TABLE_AMO_CONTACT)
public class AmoContactEntity {
  @Id private UUID id;
  private Long amoContactId;
  private String name;
  private String firstName;
  private String lastName;
  private String phone;
  // Hesap foydalanuvchi id'si (yangi tizimda UUID).
  private UUID hesapUserId;
  @CreatedDate private Instant createdDate = Instant.now();
}
