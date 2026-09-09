package uz.hesap.service.integration.repository;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import uz.hesap.service.integration.domain.RoumingSettingEntity;

public interface RoumingSettingRepository
    extends ReactiveCrudRepository<RoumingSettingEntity, UUID> {}
