package uz.hesap.service.integration.repository;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import uz.hesap.service.integration.domain.MyIdProfileEntity;

@Repository
public interface MyIdProfileRepository extends ReactiveCrudRepository<MyIdProfileEntity, UUID> {}
