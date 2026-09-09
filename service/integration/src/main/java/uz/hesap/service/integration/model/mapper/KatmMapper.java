package uz.hesap.service.integration.model.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import uz.hesap.service.integration.domain.KatmReportEntity;
import uz.hesap.service.integration.model.katm.CreditHistoryResponse;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = "spring")
public interface KatmMapper {

  // pToken sirli — javobga chiqarilmaydi. Enum → String.
  @Mapping(target = "status", expression = "java(entity.getStatus() != null ? entity.getStatus().name() : null)")
  CreditHistoryResponse toResponse(KatmReportEntity entity);
}
