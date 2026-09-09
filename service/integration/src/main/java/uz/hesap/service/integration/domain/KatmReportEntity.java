package uz.hesap.service.integration.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import uz.hesap.service.integration.domain.enums.KatmReportStatus;
import uz.hesap.service.integration.util.Constants;

// KATM kredit tarixi so'rovi va hisoboti. init-client → submit-request bosqichida
// yaratiladi (status REQUESTED), scheduler get-report orqali reportBase64'ni
// to'ldirib COMPLETED qiladi. Audit uchun so'rov/javob JSONB saqlanadi.
@Getter
@Setter
@ToString
@Table(schema = Constants.SCHEMA, name = Constants.TABLE_KATM_REPORT)
public class KatmReportEntity {
  @Id private UUID id;
  private UUID userId;
  private String pinfl;
  // KATM-SIR — KATM tomonidan berilgan mijoz identifikatori (init-client javobi).
  private String pClientId;
  // Ariza raqami (submit-request javobi).
  private String pClaimId;
  // get-report uchun kerakli token (submit-request javobi). Tashqi javobda berilmaydi.
  private String pToken;
  // Hisobot tili: uz, ru, en.
  private String language;
  private KatmReportStatus status;
  private BigDecimal amount;
  private Boolean isFree;
  private String resultMessage;
  // XML formatidagi kredit tarixi, Base64'da (get-report javobi).
  private String reportBase64;
  private Map<String, Object> request;
  private Map<String, Object> response;
  private Instant createdAt;
  private Instant completedAt;
}
