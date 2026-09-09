package uz.hesap.service.main.model.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import uz.hesap.service.main.domain.UserEntity;
import uz.hesap.service.main.model.request.EmployeeRequest;
import uz.hesap.service.main.model.response.EmployeeResponse;

@Mapper
public interface EmployeeMapper {

  EmployeeMapper INSTANCE = Mappers.getMapper(EmployeeMapper.class);

  UserEntity toEntity(EmployeeRequest request);

  EmployeeResponse toResponse(UserEntity entity);
}
