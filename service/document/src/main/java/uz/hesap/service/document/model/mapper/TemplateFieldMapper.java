package uz.hesap.service.document.model.mapper;

import org.mapstruct.*;
import org.mapstruct.factory.Mappers;
import uz.hesap.service.document.domain.template.TemplateFieldEntity;
import uz.hesap.service.document.model.request.TemplateFieldRequest;
import uz.hesap.service.document.model.response.TemplateFieldResponse;

@Mapper(
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    uses = InstantMapper.class)
public abstract class TemplateFieldMapper {

  public static final TemplateFieldMapper INSTANCE = Mappers.getMapper(TemplateFieldMapper.class);

  public abstract TemplateFieldEntity toEntity(final TemplateFieldRequest request);

  public abstract TemplateFieldResponse toResponse(final TemplateFieldEntity entity);

  public abstract void updateEntity(
      TemplateFieldRequest request, @MappingTarget TemplateFieldEntity entity);
}
