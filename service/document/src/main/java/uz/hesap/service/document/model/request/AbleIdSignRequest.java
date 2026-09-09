package uz.hesap.service.document.model.request;

// AbleID orqali imzolash: mobil SDK liveness tugagach attemptId yuboriladi
// (natija backendga webhook orqali kelib bo'lgan bo'ladi).
public record AbleIdSignRequest(String attemptId, java.util.UUID userPackageId) {}
