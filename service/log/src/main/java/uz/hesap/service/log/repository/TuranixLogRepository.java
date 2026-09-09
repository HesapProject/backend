package uz.hesap.service.log.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import uz.hesap.service.log.domain.TuranixLogEntity;

@Repository
public interface TuranixLogRepository
    extends R2dbcRepository<TuranixLogEntity, UUID>, CustomTuranixLogRepository {}
