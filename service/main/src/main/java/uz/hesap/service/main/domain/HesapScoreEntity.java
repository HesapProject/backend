package uz.hesap.service.main.domain;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import uz.hesap.service.main.util.Constants;

// Hesap Score (Faza 1) — to'lov xulq-atvoriga asoslangan ishonchlilik ko'rsatkichi.
// user_in (PINFL/STIR) bo'yicha yagona qator (upsert). JSON maydonlar TEXT sifatida.
@Getter
@Setter
@ToString
@Table(schema = Constants.SCHEMA, name = "hesap_score")
public class HesapScoreEntity {

  @Id private UUID id;
  private String userIn;
  private Integer score;
  private String band;
  private Double confidence;
  private Double coverage;
  private Boolean insufficientHistory = Boolean.FALSE;
  private Boolean coldStart = Boolean.FALSE;
  private String subScores; // JSON
  private String inputsSnapshot; // JSON
  private String reasonCodes; // JSON massiv
  private Instant computedAt;
  private Instant updatedAt;
}
