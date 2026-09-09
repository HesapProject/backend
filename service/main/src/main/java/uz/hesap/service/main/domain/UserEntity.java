package uz.hesap.service.main.domain;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import uz.hesap.service.common.util.enums.Role;
import uz.hesap.service.common.util.enums.UserType;
import uz.hesap.service.main.util.Constants;

@Getter
@Setter
@ToString
@Table(schema = Constants.SCHEMA, name = Constants.TABLE_USER)
public class UserEntity {

  @Id private UUID id;
  private String username; // For client users (C2C)
  private String firstName;
  private String lastName;
  private String midName;
  private String phone;
  private String email;
  @Column("pinfl")
  private String in;
  private String passport;
  private String passportIssuedBy; // Pasportni bergan organ (legacy give_place)
  private String passportIssueDate; // Pasport berilgan sana, ISO matn YYYY-MM-DD
  private String passportExpiryDate; // Pasport amal qilish muddati, ISO matn YYYY-MM-DD

  private UserType type;
  private Role role;
  // Admin (ADMIN/SUPER_ADMIN) login/parol — boshqa userlar telefon/OneID bilan kiradi.
  private String login;
  private String password; // BCrypt hash
  private String image; // Profile image URL
  private String bio; // User biography
  private String birthday; // Birthday in string format
  private Boolean isMan; // Gender: true = male, false = female
  private String secondPhone; // Ikkinchi telefon raqam

  private String legalName; // Yuridik shaxs to'liq nomi (CLIENT.COMPANY uchun)
  private String tin; // STIR (INN) — soliq to'lovchining identifikatsiya raqami
  private String address; // OneID manzili (per_adr) — /me'da qaytariladi
  private String region; // OneID viloyat/shahar (p_region)
  private String district; // OneID tuman (p_district)
  private String photo; // OneID pasport rasmi (base64) — /me'da qaytariladi
  private String birthPlace; // OneID tug'ilgan joyi (birth_place)
  private String nationality; // OneID millati (natn)
  private String citizenship; // OneID fuqaroligi (ctzn)
  private Boolean isVerified = Boolean.FALSE; // OneID/MyID orqali tasdiqlangan

  @CreatedDate private Instant createdDate = Instant.now();
  @LastModifiedDate private Instant lastModifiedDate = Instant.now();
  private Boolean deleted = Boolean.FALSE;
  @Version private Long version;
}
