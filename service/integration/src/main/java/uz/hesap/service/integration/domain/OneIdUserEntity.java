package uz.hesap.service.integration.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;
import uz.hesap.service.integration.util.Constants;

// OneID dan kelgan to'liq ma'lumotlar saqlanadigan table
@Getter
@Setter
@ToString
@Table(schema = Constants.SCHEMA, name = Constants.TABLE_ONE_ID_USER)
public class OneIdUserEntity implements Persistable<UUID> {

  @Id private UUID userId; // PK = user table FK

  // userId qo'lda set qilinadi (FK user.id), R2DBC default save() esa @Id
  // bo'sh emas bo'lsa har doim UPDATE qiladi — yangi qator uchun "row does
  // not exist" xatosini beradi. `Persistable.isNew()` orqali aniq aytamiz.
  @Transient private boolean newRecord = true;

  @Override
  public UUID getId() {
    return userId;
  }

  @Override
  public boolean isNew() {
    return newRecord;
  }

  // Repository.findById dan o'qib chiqilgan entity uchun update bo'lsin
  public void markNotNew() {
    this.newRecord = false;
  }

  // OneID identifikatorlar
  private String pin; // PINFL
  private String login; // OneID login (user_id)
  private Long oneIdUserId; // OneID ichki userId

  // FIO — lotin
  private String surnameLatin;
  private String nameLatin;
  private String patronymicLatin;

  // FIO — kirill
  private String surnameCyr;
  private String nameCyr;
  private String patronymicCyr;

  // FIO — ingliz
  private String surnameEn;
  private String nameEn;

  private String fullName;

  // Passport ma'lumotlari
  private String document; // passport raqam (AA1234567)
  private String docNum;
  private String issuePlace;
  private String issuePlaceId;
  private LocalDate issueDate;
  private LocalDate endDate;

  // Tug'ilgan joy
  private String birthDate;
  private String birthPlace;
  private String birthPlaceId;
  private String birthCountry;
  private String birthCountryId;

  // Shaxsiy
  private String sex;
  private String nationality;
  private String nationalityId;
  private String citizenship;
  private String citizenshipId;
  private String liveStatus;

  // Kontakt
  private String email;
  private String phone;

  // Manzil
  private String cadastre;
  private String countryId;
  private String country;
  private String regionId;
  private String region;
  private String districtId;
  private String district;
  private String address;

  // Boshqa
  private String photo;
  private String authMethod;
  private String valid;
  private String lang;
  private Boolean isActive;
  private Boolean isLegal;

  @CreatedDate private Instant createdDate = Instant.now();
  @LastModifiedDate private Instant lastModifiedDate = Instant.now();
}
