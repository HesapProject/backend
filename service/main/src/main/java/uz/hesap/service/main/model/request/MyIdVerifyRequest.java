package uz.hesap.service.main.model.request;

// MyID orqali profilni tasdiqlash: MyID'dan qaytgan code + platforma.
// platform null bo'lsa WEB (web OAuth oqimi); mobil ilova "MOBILE" yuboradi
// (mobil SDK session code'i /sdk/data orqali almashtiriladi).
public record MyIdVerifyRequest(String code, String platform) {
  public MyIdVerifyRequest {
    if (platform == null || platform.isBlank()) {
      platform = "WEB";
    }
  }
}
