package uz.hesap.service.log.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import uz.hesap.service.log.domain.ClickLogEntity;

@Repository
public interface ClickLogRepository
    extends R2dbcRepository<ClickLogEntity, UUID>, CustomClickLogRepository {}
