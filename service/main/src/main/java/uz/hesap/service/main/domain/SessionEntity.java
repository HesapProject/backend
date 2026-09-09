package uz.hesap.service.main.domain;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import uz.hesap.service.main.util.Constants;

// Session endi o'zi-yetarli: device jadvali olib tashlangan, qurilma maydonlari shu yerda.
// id = qurilma/sessiya identifikatori (JWT'dagi deviceId == sessionId).
@Getter
@Setter
@ToString
@Table(schema = Constants.SCHEMA, name = Constants.TABLE_SESSION)
public class SessionEntity {

  @Id private UUID id;

  // Verify (ro'yxatdan o'tish) sessiyasida user hali yo'q — null bo'ladi.
  private UUID userId;

  // Qurilma maydonlari (frontend login so'rovida yuboriladi).
  private String uuid;
  private String phone; // verify oqimida saqlanadi
  private String osVersion;
  private String os;
  private String model;
  private String brand;
  private String type;
  private String device;
  private String fcmToken;

  // True — ro'yxatdan o'tish (verify) sessiyasi; user yaratilgach false bo'lib qoladi.
  private Boolean isVerifyDevice = Boolean.FALSE;

  // Sessiya tugaganda (logout/admin) o'chirilmaydi — arxivlanadi.
  private Boolean archived = Boolean.FALSE;

  @CreatedDate private Instant timestamp;
}
