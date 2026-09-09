package uz.hesap.service.main.model.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import uz.hesap.service.common.util.SessionResponse;
import uz.hesap.service.main.domain.SessionEntity;
import uz.hesap.service.main.model.request.ClientOneIdVerifyRequest;
import uz.hesap.service.main.model.request.EImzoLoginRequest;
import uz.hesap.service.main.model.request.LoginRequest;
import uz.hesap.service.main.model.response.SessionAdminResponse;
import uz.hesap.service.main.model.response.VerifySessionResponse;

// Session = qurilma+sessiya (device jadvali o'rniga). Request'dan qurilma maydonlarini
// session'ga, hamda session'dan device/verify response'ga map qiladi.
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class SessionMapper {

  public static final SessionMapper INSTANCE = Mappers.getMapper(SessionMapper.class);

  // Login so'rovlaridan qurilma maydonlarini session'ga (userId/id keyinroq o'rnatiladi).
  public abstract SessionEntity toSession(LoginRequest request);

  public abstract SessionEntity toSession(EImzoLoginRequest request);

  public abstract SessionEntity toSession(ClientOneIdVerifyRequest request);

  @Mapping(target = "createdDate", source = "timestamp")
  @Mapping(target = "lastModifiedDate", ignore = true)
  public abstract SessionResponse toSessionResponse(SessionEntity session);

  public abstract VerifySessionResponse toVerifySessionResponse(SessionEntity session);

  // Admin sessiyalar tab'i — sessionId=id, createdDate/lastModifiedDate=timestamp.
  @Mapping(target = "sessionId", source = "id")
  @Mapping(target = "createdDate", source = "timestamp")
  @Mapping(target = "lastModifiedDate", source = "timestamp")
  public abstract SessionAdminResponse toSessionAdminResponse(SessionEntity session);
}
