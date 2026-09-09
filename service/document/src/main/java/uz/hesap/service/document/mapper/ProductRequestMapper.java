package uz.hesap.service.document.mapper;

import org.mapstruct.Mapper;
import uz.hesap.service.document.domain.document.ProductRequestEntity;
import uz.hesap.service.document.model.response.ProductRequestResponse;

@Mapper(componentModel = "spring")
public interface ProductRequestMapper {

  ProductRequestResponse toResponse(ProductRequestEntity entity);
}
