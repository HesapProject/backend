package uz.hesap.service.integration.model.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import uz.hesap.service.integration.domain.MyIdProfileEntity;
import uz.hesap.service.integration.model.myid.MyIdUserDataResponse;

@Mapper(componentModel = "spring")
public interface MyIdMapper {
  MyIdMapper INSTANCE = Mappers.getMapper(MyIdMapper.class);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "userIn", ignore = true)
  @Mapping(target = "createdDate", ignore = true)
  @Mapping(target = "lastModifiedDate", ignore = true)

  // Common Data
  @Mapping(source = "data.profile.commonData.firstName", target = "firstName")
  @Mapping(source = "data.profile.commonData.middleName", target = "middleName")
  @Mapping(source = "data.profile.commonData.lastName", target = "lastName")
  @Mapping(source = "data.profile.commonData.firstNameEn", target = "firstNameEn")
  @Mapping(source = "data.profile.commonData.lastNameEn", target = "lastNameEn")
  @Mapping(source = "data.profile.commonData.pinfl", target = "pinfl")
  @Mapping(source = "data.profile.commonData.gender", target = "gender")
  @Mapping(source = "data.profile.commonData.birthPlace", target = "birthPlace")
  @Mapping(source = "data.profile.commonData.birthDate", target = "birthDate")
  @Mapping(source = "data.profile.commonData.nationality", target = "nationality")
  @Mapping(source = "data.profile.commonData.citizenship", target = "citizenship")
  @Mapping(source = "data.profile.commonData.sdkHash", target = "sdkHash")
  @Mapping(source = "data.profile.commonData.lastUpdatePassData", target = "lastUpdatePassData")
  @Mapping(source = "data.profile.commonData.lastUpdateAddress", target = "lastUpdateAddress")

  // Doc Data
  @Mapping(source = "data.profile.docData.passData", target = "passData")
  @Mapping(source = "data.profile.docData.issuedBy", target = "issuedBy")
  @Mapping(source = "data.profile.docData.issuedById", target = "issuedById")
  @Mapping(source = "data.profile.docData.issuedDate", target = "issuedDate")
  @Mapping(source = "data.profile.docData.expiryDate", target = "expiryDate")
  @Mapping(source = "data.profile.docData.docType", target = "docType")
  @Mapping(source = "data.profile.docData.docTypeId", target = "docTypeId")
  @Mapping(source = "data.profile.docData.docTypeIdCbu", target = "docTypeIdCbu")

  // Contacts
  @Mapping(source = "data.profile.contacts.phone", target = "phone")
  @Mapping(source = "data.profile.contacts.email", target = "email")

  // Address
  @Mapping(source = "data.profile.address.permanentAddress", target = "permanentAddress")
  @Mapping(source = "data.profile.address.temporaryAddress", target = "temporaryAddress")

  // Permanent Registration
  @Mapping(source = "data.profile.address.permanentRegistration.mfy", target = "prMfy")
  @Mapping(source = "data.profile.address.permanentRegistration.mfyId", target = "prMfyId")
  @Mapping(source = "data.profile.address.permanentRegistration.region", target = "prRegion")
  @Mapping(source = "data.profile.address.permanentRegistration.address", target = "prAddress")
  @Mapping(source = "data.profile.address.permanentRegistration.country", target = "prCountry")
  @Mapping(source = "data.profile.address.permanentRegistration.cadastre", target = "prCadastre")
  @Mapping(source = "data.profile.address.permanentRegistration.district", target = "prDistrict")
  @Mapping(source = "data.profile.address.permanentRegistration.regionId", target = "prRegionId")
  @Mapping(source = "data.profile.address.permanentRegistration.countryId", target = "prCountryId")
  @Mapping(
      source = "data.profile.address.permanentRegistration.districtId",
      target = "prDistrictId")
  @Mapping(
      source = "data.profile.address.permanentRegistration.regionIdCbu",
      target = "prRegionIdCbu")
  @Mapping(
      source = "data.profile.address.permanentRegistration.countryIdCbu",
      target = "prCountryIdCbu")
  @Mapping(
      source = "data.profile.address.permanentRegistration.districtIdCbu",
      target = "prDistrictIdCbu")
  @Mapping(
      source = "data.profile.address.permanentRegistration.registrationDate",
      target = "prRegistrationDate")

  // Temporary Registration
  @Mapping(source = "data.profile.address.temporaryRegistration.mfy", target = "trMfy")
  @Mapping(source = "data.profile.address.temporaryRegistration.mfyId", target = "trMfyId")
  @Mapping(source = "data.profile.address.temporaryRegistration.region", target = "trRegion")
  @Mapping(source = "data.profile.address.temporaryRegistration.address", target = "trAddress")
  @Mapping(source = "data.profile.address.temporaryRegistration.cadastre", target = "trCadastre")
  @Mapping(source = "data.profile.address.temporaryRegistration.district", target = "trDistrict")
  @Mapping(source = "data.profile.address.temporaryRegistration.dateFrom", target = "trDateFrom")
  @Mapping(source = "data.profile.address.temporaryRegistration.dateTill", target = "trDateTill")
  @Mapping(source = "data.profile.address.temporaryRegistration.regionId", target = "trRegionId")
  @Mapping(
      source = "data.profile.address.temporaryRegistration.districtId",
      target = "trDistrictId")
  @Mapping(
      source = "data.profile.address.temporaryRegistration.regionIdCbu",
      target = "trRegionIdCbu")
  @Mapping(
      source = "data.profile.address.temporaryRegistration.districtIdCbu",
      target = "trDistrictIdCbu")
  MyIdProfileEntity toEntity(MyIdUserDataResponse response);
}
