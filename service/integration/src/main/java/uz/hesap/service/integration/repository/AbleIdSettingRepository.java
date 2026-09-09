package uz.hesap.service.integration.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import uz.hesap.service.integration.domain.AbleIdSettingEntity;

public interface AbleIdSettingRepository extends R2dbcRepository<AbleIdSettingEntity, UUID> {}
