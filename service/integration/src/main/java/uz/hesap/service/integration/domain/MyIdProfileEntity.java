package uz.hesap.service.integration.domain;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;
import uz.hesap.service.integration.util.Constants;

@RequiredArgsConstructor
@Getter
@Setter
@Table(schema = Constants.SCHEMA, name = Constants.TABLE_MY_ID_PROFILE)
public class MyIdProfileEntity {
  @Id private UUID id;
  // Profil egasi PINFL/STIR (user UUID o'rniga). `pinfl` esa pasport PINFL'i (alohida).
  private String userIn;
  private String firstName;
  private String middleName;
  private String lastName;
  private String firstNameEn;
  private String lastNameEn;
  private String pinfl;
  private String gender;
  private String birthPlace;
  private String birthDate;
  private String nationality;
  private String citizenship;
  private String sdkHash;
  private String lastUpdatePassData;
  private String lastUpdateAddress;
  private String passData;
  private String issuedBy;
  private String issuedById;
  private String issuedDate;
  private String expiryDate;
  private String docType;
  private String docTypeId;
  private String docTypeIdCbu;
  private String phone;
  private String email;
  private String permanentAddress;
  private String temporaryAddress;

  // Permanent Registration
  private String prMfy;
  private String prMfyId;
  private String prRegion;
  private String prAddress;
  private String prCountry;
  private String prCadastre;
  private String prDistrict;
  private String prRegionId;
  private String prCountryId;
  private String prDistrictId;
  private String prRegionIdCbu;
  private String prCountryIdCbu;
  private String prDistrictIdCbu;
  private String prRegistrationDate;

  // Temporary Registration
  private String trMfy;
  private String trMfyId;
  private String trRegion;
  private String trAddress;
  private String trCadastre;
  private String trDistrict;
  private String trDateFrom;
  private String trDateTill;
  private String trRegionId;
  private String trDistrictId;
  private String trRegionIdCbu;
  private String trDistrictIdCbu;

  @CreatedDate private Instant createdDate;
  @LastModifiedDate private Instant lastModifiedDate;
}
