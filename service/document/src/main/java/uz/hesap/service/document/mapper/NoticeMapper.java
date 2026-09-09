package uz.hesap.service.document.mapper;

import org.mapstruct.Mapper;
import uz.hesap.service.document.domain.document.NoticeEntity;
import uz.hesap.service.document.model.response.NoticeResponse;

@Mapper(componentModel = "spring")
public interface NoticeMapper {

  NoticeResponse toResponse(NoticeEntity entity);
}
