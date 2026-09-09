package uz.hesap.service.integration.model.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import uz.hesap.service.integration.domain.NotificationEntity;
import uz.hesap.service.integration.model.NotificationResponse;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

  NotificationMapper INSTANCE = Mappers.getMapper(NotificationMapper.class);

  @Mapping(target = "title.uz", source = "entity.titleUz")
  @Mapping(target = "title.ru", source = "entity.titleRu")
  @Mapping(target = "title.en", source = "entity.titleEn")
  @Mapping(target = "body.uz", source = "entity.bodyUz")
  @Mapping(target = "body.ru", source = "entity.bodyRu")
  @Mapping(target = "body.en", source = "entity.bodyEn")
  NotificationResponse toResponse(NotificationEntity entity);
}
