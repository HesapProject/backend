package uz.hesap.service.document.mapper;

import org.mapstruct.Mapper;
import uz.hesap.service.document.domain.document.ClaimEntity;
import uz.hesap.service.document.model.response.ClaimResponse;

@Mapper(componentModel = "spring")
public interface ClaimMapper {

  ClaimResponse toResponse(ClaimEntity entity);
}
