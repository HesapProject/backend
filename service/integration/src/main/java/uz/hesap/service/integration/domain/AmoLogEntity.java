package uz.hesap.service.integration.domain;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import uz.hesap.service.integration.util.Constants;

// amoCRM so'rovlari tarixi (monitoring): kontakt yaratish / token yangilash.
@Getter
@Setter
@ToString
@Table(schema = Constants.SCHEMA, name = Constants.TABLE_AMO_LOG)
public class AmoLogEntity {
  @Id private UUID id;
  private String action; // CONTACT_CREATE | TOKEN_REFRESH
  private String status; // SUCCESS | ERROR
  private String userId; // hesap foydalanuvchi id (UUID matn), token amalida null
  private String phone;
  private Long amoContactId;
  private String request;
  private String response;
  private String errorMessage;
  private Instant createdAt = Instant.now();
}
