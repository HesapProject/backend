package uz.hesap.service.log.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import uz.hesap.service.log.domain.OneIdLogEntity;

public interface OneIdLogRepository
    extends R2dbcRepository<OneIdLogEntity, UUID>, CustomOneIdLogRepository {}
