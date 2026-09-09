package uz.hesap.service.document.model.mapper;

import org.mapstruct.*;
import org.mapstruct.factory.Mappers;
import uz.hesap.service.document.domain.template.TemplateEntity;
import uz.hesap.service.document.model.request.TemplateRequest;
import uz.hesap.service.document.model.response.TemplateResponse;

@Mapper(
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    uses = InstantMapper.class)
public abstract class TemplateMapper {

  public static final TemplateMapper INSTANCE = Mappers.getMapper(TemplateMapper.class);

  public abstract TemplateEntity toEntity(final TemplateRequest request);

  public abstract TemplateResponse toResponse(final TemplateEntity entity);

  public abstract void updateEntity(TemplateRequest request, @MappingTarget TemplateEntity entity);
}
