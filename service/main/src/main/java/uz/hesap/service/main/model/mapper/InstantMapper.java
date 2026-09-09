package uz.hesap.service.main.model.mapper;

import java.time.Instant;

public class InstantMapper {

  public Long asLong(Instant instant) {
    if (instant == null) return 0L;
    return instant.toEpochMilli();
  }

  public Instant asInstant(Long milli) {
    return Instant.ofEpochMilli(milli);
  }
}
