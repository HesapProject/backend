package uz.hesap.service.integration.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import uz.hesap.service.integration.domain.ClickSettingEntity;

@Repository
public interface ClickSettingRepository extends R2dbcRepository<ClickSettingEntity, UUID> {}
