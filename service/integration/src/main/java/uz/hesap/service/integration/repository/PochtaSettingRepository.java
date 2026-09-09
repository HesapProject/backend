package uz.hesap.service.integration.repository;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import uz.hesap.service.integration.domain.PochtaSettingEntity;

public interface PochtaSettingRepository
    extends ReactiveCrudRepository<PochtaSettingEntity, UUID> {}
