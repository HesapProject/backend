package uz.hesap.service.main.model.request;

// Integration servis MyID yuz tekshiruvidan o'tgach yuboradigan passport ma'lumotlari.
// Maydon nomlari integration tarafdagi bir xil record bilan mos (Jackson JSON nomi).
public record MyIdProfileSyncRequest(
    String firstName,
    String lastName,
    String midName,
    String pinfl,
    String passport,
    String passportIssuedBy,
    String passportIssueDate,
    String passportExpiryDate,
    String birthday,
    String birthPlace,
    String nationality,
    String citizenship,
    String gender) {}
