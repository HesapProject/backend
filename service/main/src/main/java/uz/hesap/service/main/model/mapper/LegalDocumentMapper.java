package uz.hesap.service.main.model.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import uz.hesap.service.common.util.TextModel;
import uz.hesap.service.main.domain.LegalDocumentEntity;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class LegalDocumentMapper {

  public static final LegalDocumentMapper INSTANCE = Mappers.getMapper(LegalDocumentMapper.class);

  @Mapping(source = "request.uz", target = "textUz")
  @Mapping(source = "request.ru", target = "textRu")
  @Mapping(source = "request.en", target = "textEn")
  public abstract LegalDocumentEntity toEntity(TextModel request);

  @Mapping(source = "request.uz", target = "textUz")
  @Mapping(source = "request.ru", target = "textRu")
  @Mapping(source = "request.en", target = "textEn")
  public abstract LegalDocumentEntity update(LegalDocumentEntity entity, TextModel request);
}
