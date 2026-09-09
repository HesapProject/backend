package uz.hesap.service.integration.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import uz.hesap.service.integration.domain.MyIdSettingEntity;

@Repository
public interface MyIdSettingRepository extends R2dbcRepository<MyIdSettingEntity, UUID> {}
