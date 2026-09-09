package uz.hesap.service.main.model.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record OneIdUserResponse(
    Boolean authenticesp, // ?
    @JsonProperty("is_active") Boolean isActive, // ?
    @JsonProperty("is_legal") Boolean isLegal, // ?
    String valid, // ЭЦП билан тасдикланган аккаунт?
    @JsonProperty("valid_methods") List<String> validMethods,
    String lang, // ?
    //    @JsonProperty("red_date") @JsonDateTimeFormatSerializer(format = "yyyy-MM-dd HH:mm:ss")
    //        LocalDateTime createdAt,
    @JsonProperty("user_id") String login,
    String pin,
    String email,
    @JsonProperty("mob_phone_no") String phone,
    //    Status status,
    Long userId,
    @JsonProperty("doc_num") String docNum, // ?
    @JsonProperty("full_name") String fullName,
    @JsonProperty("pport_no") String document,
    @JsonProperty("sur_name") String surnameLatin,
    @JsonProperty("first_name") String nameLatin,
    @JsonProperty("mid_name") String patronymicLatin,
    @JsonProperty("surname_engl") String surnameEn,
    @JsonProperty("name_engl") String nameEn,
    @JsonProperty("surname_cyr") String surnameCyr,
    @JsonProperty("name_cyr") String nameCyr,
    @JsonProperty("patronymic_cyr") String patronymicCyr,
    @JsonProperty("birth_date") String birthDate,
    @JsonProperty("birth_place") String birthPlace,
    @JsonProperty("birth_place_id") String birthPlaceId,
    @JsonProperty("birth_cntry") String birthCountry,
    @JsonProperty("birth_country_id") String birthCountryId,
    @JsonProperty("livestatus") String liveStatus,
    @JsonProperty("natn") String nationality,
    @JsonProperty("nationality_id") String nationalityId,
    @JsonProperty("ctzn") String citizenship,
    @JsonProperty("citizenship_id") String citizenshipId,
    @JsonProperty("gd") String sex,
    @JsonProperty("pport_issue_place") String issuePlace,
    @JsonProperty("doc_give_place_id") String issuePlaceId,
    @JsonProperty("pport_issue_date")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate issueDate,
    @JsonProperty("_pport_issue_date") String issueDate2,
    @JsonProperty("pport_expr_date")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate endDate,
    @JsonProperty("_pport_expr_date") String endDate2,
    String photo,
    @JsonProperty("p_regdate")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime registrationDate,
    @JsonProperty("p_kadastr") String cadastre,
    @JsonProperty("p_countryid") String countryId,
    @JsonProperty("p_country") String country,
    @JsonProperty("p_regionid") String regionId,
    @JsonProperty("p_region") String region,
    @JsonProperty("p_districtid") String districtId,
    @JsonProperty("p_district") String district,
    @JsonProperty("per_adr") String address,
    //        @JsonProperty("user_type") ScopeType scopeType,
    //        @JsonProperty("sess_id") UUID accessTokenId,
    @JsonProperty("ret_cd") String retCd, // 0 ?
    @JsonProperty("auth_method") String authMethod, // ?
    @JsonProperty("pkcs_legal_tin") String pkcsLegalTin, // ?
    @JsonProperty("legal_info") List<LegalInfo> legalInfo,
    @JsonProperty("registered_phones") List<String> registeredPhones) {
  public record LegalInfo(
      @JsonProperty("is_basic") boolean isBasic,
      String tin,
      @JsonProperty("acron_UZ") String acronUz,
      @JsonProperty("le_tin") String leTin,
      @JsonProperty("le_name") String leName) {}
}
