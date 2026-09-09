package uz.hesap.service.main.model.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import uz.hesap.service.common.util.TextModel;
import uz.hesap.service.main.domain.FaqEntity;
import uz.hesap.service.main.model.FaqRequest;
import uz.hesap.service.main.model.FaqResponse;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class FaqMapper {

  public static final FaqMapper INSTANCE = Mappers.getMapper(FaqMapper.class);

  @Mapping(target = "title", expression = "java(mapTitle(entity))")
  @Mapping(target = "answer", expression = "java(mapAnswer(entity))")
  public abstract FaqResponse toResponse(FaqEntity entity);

  @Mapping(source = "title.uz", target = "titleUz")
  @Mapping(source = "title.ru", target = "titleRu")
  @Mapping(source = "title.en", target = "titleEn")
  @Mapping(source = "answer.uz", target = "answerUz")
  @Mapping(source = "answer.ru", target = "answerRu")
  @Mapping(source = "answer.en", target = "answerEn")
  public abstract FaqEntity toEntity(FaqRequest request);

  @Mapping(source = "title.uz", target = "titleUz")
  @Mapping(source = "title.ru", target = "titleRu")
  @Mapping(source = "title.en", target = "titleEn")
  @Mapping(source = "answer.uz", target = "answerUz")
  @Mapping(source = "answer.ru", target = "answerRu")
  @Mapping(source = "answer.en", target = "answerEn")
  public abstract void updateEntity(FaqRequest request, @MappingTarget FaqEntity entity);

  protected TextModel mapTitle(FaqEntity entity) {
    return new TextModel(entity.getTitleUz(), entity.getTitleRu(), entity.getTitleEn());
  }

  protected TextModel mapAnswer(FaqEntity entity) {
    return new TextModel(entity.getAnswerUz(), entity.getAnswerRu(), entity.getAnswerEn());
  }
}
