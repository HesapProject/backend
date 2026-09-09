package uz.hesap.service.main.model;

import java.io.Serializable;
import java.time.Instant;

// SMS verify flow uchun cache (sign-up). Parol field olib tashlandi.
public record ClientCacheModel(
    String firstName, String lastName, String phone, String code, String action, Instant sendTime)
    implements Serializable {

  public static final String ACTION_CREATE = "create";
  public static final String ACTION_RECOVERY = "recovery";
}
