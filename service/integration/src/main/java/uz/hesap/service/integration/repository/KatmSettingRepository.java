package uz.hesap.service.integration.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import uz.hesap.service.integration.domain.KatmSettingEntity;

@Repository
public interface KatmSettingRepository extends R2dbcRepository<KatmSettingEntity, UUID> {}
