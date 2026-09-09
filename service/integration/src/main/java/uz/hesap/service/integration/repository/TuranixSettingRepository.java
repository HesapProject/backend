package uz.hesap.service.integration.repository;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import uz.hesap.service.integration.domain.TuranixSettingEntity;

public interface TuranixSettingRepository
    extends ReactiveCrudRepository<TuranixSettingEntity, UUID> {}
