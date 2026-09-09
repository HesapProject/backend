package uz.hesap.service.main.model.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import uz.hesap.service.common.util.TextModel;
import uz.hesap.service.main.domain.StoryEntity;
import uz.hesap.service.main.model.StoryItem;
import uz.hesap.service.main.model.StoryRequest;
import uz.hesap.service.main.model.StoryResponse;
import uz.hesap.service.main.repository.StoryRepository.StoryProjection;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class StoryMapper {

  public static final StoryMapper INSTANCE = Mappers.getMapper(StoryMapper.class);
  private static final ObjectMapper objectMapper = new ObjectMapper();

  @Mapping(target = "title", expression = "java(mapTitle(entity))")
  @Mapping(target = "items", source = "items", qualifiedByName = "jsonToItems")
  @Mapping(target = "isViewed", ignore = true)
  public abstract StoryResponse toResponse(StoryEntity entity);

  public StoryResponse projectionToResponse(StoryProjection p) {
    TextModel title = new TextModel(p.getTitleUz(), p.getTitleRu(), p.getTitleEn());
    List<StoryItem> items = jsonToItems(p.getItems());
    return new StoryResponse(
        p.getId(), title, p.getAvatar(), items, p.getIsViewed(), p.getCreatedDate());
  }

  @Mapping(source = "title.uz", target = "titleUz")
  @Mapping(source = "title.ru", target = "titleRu")
  @Mapping(source = "title.en", target = "titleEn")
  @Mapping(target = "items", source = "items", qualifiedByName = "itemsToJson")
  public abstract StoryEntity toEntity(StoryRequest request);

  @Mapping(source = "title.uz", target = "titleUz")
  @Mapping(source = "title.ru", target = "titleRu")
  @Mapping(source = "title.en", target = "titleEn")
  @Mapping(target = "items", source = "items", qualifiedByName = "itemsToJson")
  public abstract void updateEntity(StoryRequest request, @MappingTarget StoryEntity entity);

  protected TextModel mapTitle(StoryEntity entity) {
    return new TextModel(entity.getTitleUz(), entity.getTitleRu(), entity.getTitleEn());
  }

  @Named("jsonToItems")
  protected List<StoryItem> jsonToItems(String json) {
    if (json == null || json.isBlank()) return Collections.emptyList();
    try {
      return objectMapper.readValue(json, new TypeReference<>() {});
    } catch (JsonProcessingException e) {
      return Collections.emptyList();
    }
  }

  @Named("itemsToJson")
  protected String itemsToJson(List<StoryItem> items) {
    if (items == null || items.isEmpty()) return "[]";
    try {
      return objectMapper.writeValueAsString(items);
    } catch (JsonProcessingException e) {
      return "[]";
    }
  }
}
