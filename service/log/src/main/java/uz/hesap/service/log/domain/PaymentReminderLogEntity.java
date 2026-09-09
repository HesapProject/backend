package uz.hesap.service.log.domain;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import uz.hesap.service.log.util.Constants;

// PaymentCronService har kunlik "to'lov eslatmasi" cron run tarixi:
// startedAt, completedAt (yakun), topilgan/muvaffaqiyatli/xato son'lari + xato xabari.
@RequiredArgsConstructor
@Getter
@Setter
@Table(schema = Constants.SCHEMA, name = Constants.TABLE_PAYMENT_REMINDER_LOG)
public class PaymentReminderLogEntity {
  @Id private UUID id;
  private Instant startedAt;
  // completedAt null → cron hali tugamagan yoki xato bilan uzilgan.
  private Instant completedAt;
  // Kandidatlar soni (to'lov muddati yaqin/o'tgan shartnomalar).
  private Integer totalCandidates;
  // Muvaffaqiyatli yuborilgan eslatmalar.
  private Integer sentSuccess;
  // Xato bilan yuborilmagan eslatmalar.
  private Integer sentFailed;
  // Umumiy cron xatosi (stack trace/message) — mavjud bo'lsa.
  private String errorMessage;
}
