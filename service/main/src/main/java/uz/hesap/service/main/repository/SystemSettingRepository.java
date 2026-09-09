package uz.hesap.service.main.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import uz.hesap.service.main.domain.SystemSettingEntity;

@Repository
public interface SystemSettingRepository extends R2dbcRepository<SystemSettingEntity, String> {}
