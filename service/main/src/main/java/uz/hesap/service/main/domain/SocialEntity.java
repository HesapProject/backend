package uz.hesap.service.main.domain;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import uz.hesap.service.main.util.Constants;

@Getter
@Setter
@ToString
@Table(schema = Constants.SCHEMA, name = Constants.TABLE_SOCIAL)
public class SocialEntity {

  @Id private UUID id;
  // Egasi PINFL/STIR (user UUID o'rniga).
  private String userIn;
  private String phone2;
  private String telegram;
  private String instagram;
  private String facebook;
}
