package uz.hesap.service.integration.promos;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface PromosMapper {

  PromosEntity toEntity(PromosRequest request);

  PromosResponse toResponse(PromosEntity entity);

  void toUpdate(PromosRequest request, @MappingTarget PromosEntity entity);
}
