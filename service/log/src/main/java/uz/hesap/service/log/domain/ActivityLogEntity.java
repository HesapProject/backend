package uz.hesap.service.log.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;
import uz.hesap.service.common.util.enums.Activity;
import uz.hesap.service.log.util.Constants;

@RequiredArgsConstructor
@Getter
@Setter
@Table(schema = Constants.SCHEMA, name = Constants.TABLE_ACTIVITY_LOG)
public class ActivityLogEntity {
  @Id private UUID id;
  // Amalni bajargan foydalanuvchi IN (PINFL/STIR), user_id emas.
  private String actorIn;
  // Kompaniya STIR (kompaniya konteksti bo'lsa), company_id emas.
  private String companyIn;
  // Amal turi (text ustun, enum nomi saqlanadi).
  private Activity activity;
  // Amalga oid tafsilotlar JSON (Map -> json ustun, user_log'dagidek).
  private Map<String, Object> data;
  @CreatedDate private Instant createdDate;
  @LastModifiedDate private Instant lastModifiedDate;
}
