package uz.hesap.service.integration.model.user;

// MyID yuz tekshiruvidan o'tgach user servisdagi user jadvalini yangilash uchun
// yuboriladigan passport ma'lumotlari. Maydon nomlari user-domain bilan bir xil
// (Jackson JSON nomi bo'yicha bog'lanadi).
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
