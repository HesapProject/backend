package uz.hesap.service.integration.model.myid;

import java.util.UUID;

// document-service'dan keladi: MyID code'ni imzolash uchun tekshirish (s2s).
// platform null bo'lsa MOBILE. WEB -> web SDK credential, MOBILE -> mobil SDK.
public record MyIdVerifyRequest(String code, UUID userId, MyIdPlatform platform) {
  public MyIdVerifyRequest {
    if (platform == null) {
      platform = MyIdPlatform.MOBILE;
    }
  }
}
