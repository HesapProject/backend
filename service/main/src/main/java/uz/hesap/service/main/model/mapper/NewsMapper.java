package uz.hesap.service.main.model.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import uz.hesap.service.common.util.TextModel;
import uz.hesap.service.main.domain.NewsEntity;
import uz.hesap.service.main.model.NewsRequest;
import uz.hesap.service.main.model.NewsResponse;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class NewsMapper {

  public static final NewsMapper INSTANCE = Mappers.getMapper(NewsMapper.class);

  @Mapping(target = "title", expression = "java(mapTitle(entity))")
  @Mapping(target = "body", expression = "java(mapBody(entity))")
  public abstract NewsResponse toResponse(NewsEntity entity);

  @Mapping(source = "title.uz", target = "titleUz")
  @Mapping(source = "title.ru", target = "titleRu")
  @Mapping(source = "title.en", target = "titleEn")
  @Mapping(source = "body.uz", target = "bodyUz")
  @Mapping(source = "body.ru", target = "bodyRu")
  @Mapping(source = "body.en", target = "bodyEn")
  public abstract NewsEntity toEntity(NewsRequest request);

  @Mapping(source = "title.uz", target = "titleUz")
  @Mapping(source = "title.ru", target = "titleRu")
  @Mapping(source = "title.en", target = "titleEn")
  @Mapping(source = "body.uz", target = "bodyUz")
  @Mapping(source = "body.ru", target = "bodyRu")
  @Mapping(source = "body.en", target = "bodyEn")
  public abstract void updateEntity(NewsRequest request, @MappingTarget NewsEntity entity);

  protected TextModel mapTitle(NewsEntity entity) {
    return new TextModel(entity.getTitleUz(), entity.getTitleRu(), entity.getTitleEn());
  }

  protected TextModel mapBody(NewsEntity entity) {
    return new TextModel(entity.getBodyUz(), entity.getBodyRu(), entity.getBodyEn());
  }
}
