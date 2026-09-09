package uz.hesap.service.integration.model.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import uz.hesap.service.integration.domain.TuranixCheckEntity;
import uz.hesap.service.integration.model.turanix.TuranixCheckResponse;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = "spring")
public interface TuranixMapper {

  // request/response JSONB tashqi javobga chiqarilmaydi.
  TuranixCheckResponse toResponse(TuranixCheckEntity entity);
}
