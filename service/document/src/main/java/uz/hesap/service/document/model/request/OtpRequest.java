package uz.hesap.service.document.model.request;

import java.util.UUID;
import uz.hesap.service.common.exception.InvalidOperationException;

// userPackageId — ixtiyoriy: yaratuvchi imzolashda tanlagan paket (billing imzolashda).
public record OtpRequest(Integer code, UUID userPackageId) {
  public OtpRequest {
    // 5 xonali kod: 10000–99999.
    if (!(code != null && code > 9999 && code < 100000)) {
      throw new InvalidOperationException("Invalid code");
    }
  }
}
