package uz.hesap.service.main.model.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import uz.hesap.service.main.domain.PackageEntity;
import uz.hesap.service.main.model.PackageRequest;

@Mapper(componentModel = "spring")
public interface PackageMapper {

  @Mapping(target = "nameUz", source = "name.uz")
  @Mapping(target = "nameRu", source = "name.ru")
  @Mapping(target = "nameEn", source = "name.en")
  @Mapping(target = "descriptionUz", source = "description.uz")
  @Mapping(target = "descriptionRu", source = "description.ru")
  @Mapping(target = "descriptionEn", source = "description.en")
  @Mapping(target = "templates", ignore = true) // JSON alohida serialize qilinadi
  PackageEntity toEntity(PackageRequest request);

  @Mapping(target = "nameUz", source = "name.uz")
  @Mapping(target = "nameRu", source = "name.ru")
  @Mapping(target = "nameEn", source = "name.en")
  @Mapping(target = "descriptionUz", source = "description.uz")
  @Mapping(target = "descriptionRu", source = "description.ru")
  @Mapping(target = "descriptionEn", source = "description.en")
  @Mapping(target = "templates", ignore = true)
  void toUpdate(PackageRequest request, @MappingTarget PackageEntity entity);
}
