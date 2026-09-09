package uz.hesap.service.main.model.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import uz.hesap.service.common.util.UserBasicResponse;
import uz.hesap.service.main.domain.PermissionRequestEntity;
import uz.hesap.service.main.domain.UserEntity;
import uz.hesap.service.main.domain.UserPermissionEntity;
import uz.hesap.service.main.model.response.PermissionRequestResponse;
import uz.hesap.service.main.model.response.UserPermissionResponse;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = InstantMapper.class)
public abstract class PermissionRequestMapper {

  public static final PermissionRequestMapper INSTANCE =
      Mappers.getMapper(PermissionRequestMapper.class);

  @Mapping(target = "userFrom", source = "userFrom")
  @Mapping(target = "userTo", source = "userTo")
  @Mapping(target = "id", source = "entity.id")
  @Mapping(target = "passport", source = "entity.passport")
  @Mapping(target = "payability", source = "entity.payability")
  @Mapping(target = "contract", source = "entity.contract")
  @Mapping(target = "partner", source = "entity.partner")
  @Mapping(target = "status", source = "entity.status")
  @Mapping(target = "createdDate", source = "entity.createdDate")
  @Mapping(target = "lastModifiedDate", source = "entity.lastModifiedDate")
  public abstract PermissionRequestResponse toResponse(
      PermissionRequestEntity entity, UserBasicResponse userFrom, UserBasicResponse userTo);

  @Mapping(target = "userFrom", source = "userFrom")
  @Mapping(target = "userTo", source = "userTo")
  @Mapping(target = "id", source = "entity.id")
  @Mapping(target = "createdDate", source = "entity.createdDate")
  @Mapping(target = "lastModifiedDate", source = "entity.lastModifiedDate")
  public abstract UserPermissionResponse toUserPermissionResponse(
      UserPermissionEntity entity, UserBasicResponse userFrom, UserBasicResponse userTo);

  public abstract UserPermissionResponse toPermissionResponse(UserPermissionEntity entity);

  public abstract UserBasicResponse toUserBasicResponse(UserEntity entity);
}
