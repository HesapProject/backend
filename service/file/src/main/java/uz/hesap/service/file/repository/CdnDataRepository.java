package uz.hesap.service.file.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import uz.hesap.service.file.domain.CdnDataEntity;

public interface CdnDataRepository extends R2dbcRepository<CdnDataEntity, UUID> {}
