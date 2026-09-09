package uz.hesap.service.integration.model;

/**
 * Bir hodisa uchun yechilgan (resolved) notification sozlamasi. Config qatori bo'lmasa
 * {@link #defaults()} qaytariladi: push yoniq, SMS o'chiq (eski xulqni saqlash).
 */
public record NotificationConfig(
    boolean smsEnabled, String smsText, boolean firebaseEnabled, String firebaseText) {

  public static NotificationConfig defaults() {
    return new NotificationConfig(false, null, true, null);
  }
}
