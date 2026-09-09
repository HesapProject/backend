package uz.hesap.service.main.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import uz.hesap.service.main.util.Constants;

// Tizim sozlamasi (key-value). String @Id bo'lgani uchun INSERT/UPDATE'ni
// Persistable.isNew() bilan boshqaramiz (yangi qator uchun true).
@Getter
@Setter
@Table(schema = Constants.SCHEMA, name = Constants.TABLE_SYSTEM_SETTING)
public class SystemSettingEntity implements Persistable<String> {

  @Id
  @Column("setting_key")
  private String settingKey;

  @Column("setting_value")
  private String settingValue;

  @Transient private boolean newRow;

  @Override
  public String getId() {
    return settingKey;
  }

  @Override
  public boolean isNew() {
    return newRow;
  }
}
