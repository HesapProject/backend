package uz.hesap.service.log.repository;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import uz.hesap.service.log.domain.EskizLogEntity;

@Repository
public interface EskizLogRepository
    extends ReactiveCrudRepository<EskizLogEntity, UUID>, CustomEskizLogRepository {}
