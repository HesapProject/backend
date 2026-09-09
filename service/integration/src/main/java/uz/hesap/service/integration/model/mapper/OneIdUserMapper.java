package uz.hesap.service.integration.model.mapper;

import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import uz.hesap.service.integration.domain.OneIdUserEntity;
import uz.hesap.service.integration.model.oneid.OneIdPassportResponse;
import uz.hesap.service.integration.model.oneid.OneIdUserResponse;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class OneIdUserMapper {

  public static final OneIdUserMapper INSTANCE = Mappers.getMapper(OneIdUserMapper.class);

  // OneIdUserResponse → OneIdUserEntity (userId = PK)
  @Mapping(target = "userId", source = "userId")
  public abstract OneIdUserEntity toEntity(UUID userId, OneIdUserResponse response);

  public abstract OneIdPassportResponse toPassportResponse(OneIdUserEntity entity);
}
