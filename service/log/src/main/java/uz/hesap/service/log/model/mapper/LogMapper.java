package uz.hesap.service.log.model.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import uz.hesap.service.common.util.message.EskizLogReply;
import uz.hesap.service.log.domain.ActivityLogEntity;
import uz.hesap.service.log.domain.ClickLogEntity;
import uz.hesap.service.log.domain.EskizLogEntity;
import uz.hesap.service.log.domain.MyIdLogEntity;
import uz.hesap.service.log.domain.OneIdLogEntity;
import uz.hesap.service.log.domain.PaymeLogEntity;
import uz.hesap.service.log.domain.PlumLogEntity;
import uz.hesap.service.log.domain.TuranixLogEntity;
import uz.hesap.service.log.model.ClickLogResponse;
import uz.hesap.service.log.model.EskizLogResponse;
import uz.hesap.service.log.model.MyIdLogResponse;
import uz.hesap.service.log.model.OneIdLogResponse;
import uz.hesap.service.log.model.PaymeLogResponse;
import uz.hesap.service.log.model.PlumLogResponse;
import uz.hesap.service.log.model.ActivityLogResponse;
import uz.hesap.service.log.model.TuranixLogResponse;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = InstantMapper.class)
public abstract class LogMapper {

  public static final LogMapper INSTANCE = Mappers.getMapper(LogMapper.class);

  public abstract OneIdLogResponse toResponse(OneIdLogEntity entity);

  public abstract EskizLogResponse toResponse(EskizLogEntity entity);

  public abstract MyIdLogResponse toResponse(MyIdLogEntity entity);

  public abstract PlumLogResponse toResponse(PlumLogEntity entity);

  public abstract PaymeLogResponse toResponse(PaymeLogEntity entity);

  public abstract ClickLogResponse toResponse(ClickLogEntity entity);

  public abstract TuranixLogResponse toResponse(TuranixLogEntity entity);

  public abstract ActivityLogResponse toResponse(ActivityLogEntity entity);

  public abstract EskizLogEntity toEntity(EskizLogReply reply);
}
