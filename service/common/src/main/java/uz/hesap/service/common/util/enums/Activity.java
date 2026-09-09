package uz.hesap.service.common.util.enums;

/**
 * Foydalanuvchi amallari (activity_log). Har bir ish uchun alohida qiymat — yangi amal qo'shilsa
 * shu yerga qiymat qo'shiladi. Activity nomi text ustunda saqlanadi (native enum emas).
 */
public enum Activity {
  // Auth
  LOGIN,
  LOGOUT,

  // Shartnoma
  CONTRACT_CREATE,
  CONTRACT_EDIT,
  CONTRACT_SIGN,
  CONTRACT_REJECT,
  CONTRACT_CANCEL,
  CONTRACT_COMPLETE,
  CONTRACT_DELETE,

  // To'lov
  PAYMENT_REQUEST_CREATE,
  PAYMENT_PAY,
  PAYMENT_APPROVE,
  PAYMENT_REJECT,
  DELAY_REQUEST_CREATE,

  // Talabnoma / da'vo
  NOTICE_CREATE,
  CLAIM_CREATE,

  // Guvohlik
  WITNESS_ACCEPT,
  WITNESS_REJECT,

  // Shablon
  TEMPLATE_CREATE,
  TEMPLATE_UPDATE,
  TEMPLATE_DELETE,
  TEMPLATE_APPLICATION_CREATE,

  // Ruxsat (white_list)
  PERMISSION_REQUEST,
  PERMISSION_ACCEPT,
  PERMISSION_REJECT,

  // Profil
  PROFILE_UPDATE,
  PASSPORT_UPDATE,

  // Kompaniya / xodim
  STAFF_REQUEST,
  STAFF_ACCEPT,
  STAFF_REJECT,
  ACT_AS_COMPANY,

  // Paket / balans
  PACKAGE_PURCHASE,
  PACKAGE_GRANT,
  BALANCE_TOPUP
}
