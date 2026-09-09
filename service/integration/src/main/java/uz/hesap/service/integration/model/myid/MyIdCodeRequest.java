package uz.hesap.service.integration.model.myid;

// code → user data. platform null bo'lsa MOBILE deb qabul qilinadi.
public record MyIdCodeRequest(String code, MyIdPlatform platform) {
  public MyIdCodeRequest {
    if (platform == null) {
      platform = MyIdPlatform.MOBILE;
    }
  }
}
