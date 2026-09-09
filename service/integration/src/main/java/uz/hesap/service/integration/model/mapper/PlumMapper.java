package uz.hesap.service.integration.model.mapper;

import java.util.Map;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import uz.hesap.service.integration.domain.PlumScoringEntity;
import uz.hesap.service.integration.domain.UserCardEntity;
import uz.hesap.service.integration.model.CardResponse;
import uz.hesap.service.integration.model.plum.ScoringResponse;
import uz.hesap.service.integration.model.plum.ScoringStatusResponse;

@Mapper(
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    uses = InstantMapper.class,
    componentModel = "spring")
public interface PlumMapper {

  PlumMapper INSTANCE = Mappers.getMapper(PlumMapper.class);

  // result — response HECH QACHON to'ldirilmaydi; HUMO natija uzcard'da, UZCARD boshqacha.
  // Shu sabab bo'sh bo'lmagan xaritani tanlaymiz (aks holda iOS'da "no data").
  @Mapping(target = "result", expression = "java(pickResult(entity))")
  ScoringStatusResponse toScoringStatusResponse(PlumScoringEntity entity);

  default Map<String, Object> pickResult(PlumScoringEntity e) {
    if (e.getResponse() != null && !e.getResponse().isEmpty()) return e.getResponse();
    if (e.getUzcard() != null && !e.getUzcard().isEmpty()) return e.getUzcard();
    return e.getHumo();
  }

  ScoringResponse toScoringResponse(PlumScoringEntity entity);

  CardResponse toCardResponse(UserCardEntity card);
}
