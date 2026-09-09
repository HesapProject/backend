package uz.hesap.service.log.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import uz.hesap.service.log.domain.ActivityLogEntity;

@Repository
public interface ActivityLogRepository
    extends R2dbcRepository<ActivityLogEntity, UUID>, CustomActivityLogRepository {}
