package uz.hesap.service.common.util.enums;

public enum UserType {

  /** SuperAdmin - HESAP super administrator (to'liq huquq) */
  SUPER_ADMIN,

  /** Admin - HESAP administrator */
  ADMIN,

  /** Client - Mobile app individual user (C2C from hesap) */
  CLIENT,

  /**
   * Company - Legal entity (yuridik shaxs) authenticated via OneID PKCS. OneID javobida
   * `isLegal=true` va `pkcsLegalTin` (9-xonali) bo'ladi — pinfl o'rniga shu TIN identifikator
   * sifatida ishlatiladi.
   */
  COMPANY
}
