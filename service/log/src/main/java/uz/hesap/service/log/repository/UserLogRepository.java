package uz.hesap.service.log.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import uz.hesap.service.log.domain.UserLogEntity;

public interface UserLogRepository extends R2dbcRepository<UserLogEntity, UUID> {}
