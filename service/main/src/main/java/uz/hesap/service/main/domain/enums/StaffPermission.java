package uz.hesap.service.main.domain.enums;

// Staff ruxsatlari. Saqlash: vergul bilan ajratilgan nomlar (StaffEntity.permissions TEXT).
public enum StaffPermission {
  Statistics,
  CheckUser,
  Contract_Read,
  Contract_Create,
  Contract_Sign,
  Contract_Reject,
  Contract_Close,
  Reports,
  PaymentSchedule,
  ProductSchedule,
  Partners,
  Billing_Read,
  Billing_Buy
}
