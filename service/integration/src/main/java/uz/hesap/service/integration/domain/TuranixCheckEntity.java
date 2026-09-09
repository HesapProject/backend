package uz.hesap.service.integration.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import uz.hesap.service.integration.util.Constants;

// Turanix check-pass-msisdn so'rovi tarixi: MSISDN pasport/PINFL'ga biriktirilganini
// tekshirish natijasi. Audit uchun so'rov/javob JSONB saqlanadi. Har so'rov log
// servisga ham yuboriladi (TuranixLogReply).
@Getter
@Setter
@ToString
@Table(schema = Constants.SCHEMA, name = Constants.TABLE_TURANIX_CHECK)
public class TuranixCheckEntity {
  @Id private UUID id;
  private UUID userId;
  private String msisdn;
  private String pinfl;
  private String passSer;
  private String passNum;
  // SUCCESS / ERROR — tashqi so'rov muvaffaqiyatli ketdimi.
  private String status;
  // Turanix javobidagi natija kodi (masalan 3000).
  private Integer resultCode;
  // Turanix javobidagi izoh.
  private String description;
  private String errorMessage;
  private Map<String, Object> request;
  private Map<String, Object> response;
  private Instant createdAt;
}
