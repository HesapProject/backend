package uz.hesap.service.log.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import uz.hesap.service.log.domain.MyIdLogEntity;

@Repository
public interface MyIdLogRepository
    extends R2dbcRepository<MyIdLogEntity, UUID>, CustomMyIdLogRepository {}
