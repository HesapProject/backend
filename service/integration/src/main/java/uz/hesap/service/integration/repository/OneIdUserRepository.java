package uz.hesap.service.integration.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import uz.hesap.service.integration.domain.OneIdUserEntity;

@Repository
public interface OneIdUserRepository extends R2dbcRepository<OneIdUserEntity, UUID> {
  // PK = userId, shuning uchun findById(userId) ishlatiladi
}
