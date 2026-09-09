package uz.hesap.service.main.model.mapper;

import java.util.List;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;
import uz.hesap.service.common.util.*;
import uz.hesap.service.common.util.enums.Role;
import uz.hesap.service.main.domain.*;
import uz.hesap.service.main.model.UserCacheModel;
import uz.hesap.service.main.model.request.AdminUserUpdateRequest;
import uz.hesap.service.main.model.request.ClientProfileUpdateRequest;
import uz.hesap.service.main.model.request.UserRequest;
import uz.hesap.service.main.model.request.UserUpdateRequest;
import uz.hesap.service.main.model.response.AdminUserResponse;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = InstantMapper.class)
public abstract class UserMapper {

  public static final UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

  public abstract UserEntity toUserEntity(final UserRequest request);

  public abstract UserEntity toUserEntity(final UserCacheModel request);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  public abstract UserEntity toUserEntity(
      final @MappingTarget UserEntity entity, final UserUpdateRequest request);

  // C2C profil tahrirlash: null fieldlar o'zgartirilmaydi
  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  public abstract UserEntity updateProfileFromRequest(
      final @MappingTarget UserEntity entity, final ClientProfileUpdateRequest request);

  @Mapping(target = "companies", source = "companies")
  @Mapping(target = "sessions", source = "sessions")
  public abstract AdminUserResponse toAdminUserResponse(
      final UserEntity userEntity,
      List<AdminUserResponse.CompanyInfo> companies,
      List<SessionEntity> sessions);

  // Admin ro'yxati uchun — agregatlar (shartnoma soni/balans/oxirgi tashrif) bilan.
  @Mapping(target = "companies", source = "companies")
  @Mapping(target = "sessions", source = "sessions")
  @Mapping(target = "contractsCount", source = "contractsCount")
  @Mapping(target = "balance", source = "balance")
  @Mapping(target = "lastVisitDate", source = "lastVisitDate")
  public abstract AdminUserResponse toAdminUserResponse(
      final UserEntity userEntity,
      List<AdminUserResponse.CompanyInfo> companies,
      List<SessionEntity> sessions,
      Integer contractsCount,
      Double balance,
      java.time.Instant lastVisitDate);

  @Mapping(target = "role", ignore = true)
  @Mapping(target = "document", source = "passport")
  @Mapping(target = "verified", source = "isVerified")
  public abstract UserResponse toUserResponse(final UserEntity userEntity);

  @Mapping(target = "verified", source = "user.isVerified")
  @Mapping(target = "device", source = "device")
  @Mapping(target = "id", source = "user.id")
  @Mapping(target = "createdDate", source = "user.createdDate")
  @Mapping(target = "lastModifiedDate", source = "user.lastModifiedDate")
  @Mapping(target = "role", source = "role")
  @Mapping(target = "company", source = "company")
  @Mapping(target = "type", source = "user.type")
  @Mapping(target = "tin", source = "user.tin")
  @Mapping(target = "legalName", source = "user.legalName")
  @Mapping(target = "address", source = "user.address")
  @Mapping(target = "region", source = "user.region")
  @Mapping(target = "district", source = "user.district")
  @Mapping(target = "photo", source = "user.photo")
  @Mapping(target = "document", source = "user.passport")
  @Mapping(target = "birthday", source = "user.birthday")
  @Mapping(target = "birthPlace", source = "user.birthPlace")
  @Mapping(target = "passportIssuedBy", source = "user.passportIssuedBy")
  @Mapping(target = "passportIssueDate", source = "user.passportIssueDate")
  @Mapping(target = "passportExpiryDate", source = "user.passportExpiryDate")
  @Mapping(target = "nationality", source = "user.nationality")
  @Mapping(target = "citizenship", source = "user.citizenship")
  public abstract UserResponse toUserResponse(
      final UserEntity user, final SessionResponse device, final Role role, CompanyResponse company);

  public abstract UserEntity updateUserFromRequest(
      AdminUserUpdateRequest request, @MappingTarget UserEntity user);

  public abstract UserBasicResponse toUserBasicResponse(final UserEntity userEntity);
}
