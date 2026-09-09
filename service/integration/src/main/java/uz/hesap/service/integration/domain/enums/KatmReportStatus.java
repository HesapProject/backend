package uz.hesap.service.integration.domain.enums;

// KATM kredit tarixi so'rovi holati.
// REQUESTED — submit-request yuborildi, hisobot tayyor bo'lishi kutilmoqda
// (scheduler get-report orqali tekshiradi); COMPLETED — reportBase64 olindi;
// FAILED — xatolik.
public enum KatmReportStatus {
  REQUESTED,
  COMPLETED,
  FAILED
}
