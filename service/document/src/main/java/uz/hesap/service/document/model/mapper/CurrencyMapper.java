package uz.hesap.service.document.model.mapper;

import org.mapstruct.*;
import org.mapstruct.factory.Mappers;
import uz.hesap.service.document.domain.CurrencyEntity;
import uz.hesap.service.document.model.request.CurrencyRequest;
import uz.hesap.service.document.model.response.CurrencyResponse;

@Mapper(
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    uses = InstantMapper.class)
public abstract class CurrencyMapper {

  public static final CurrencyMapper INSTANCE = Mappers.getMapper(CurrencyMapper.class);

  public abstract CurrencyEntity toEntity(final CurrencyRequest request);

  public abstract void updateEntity(CurrencyRequest request, @MappingTarget CurrencyEntity entity);

  public abstract CurrencyResponse toResponse(final CurrencyEntity entity);
}
