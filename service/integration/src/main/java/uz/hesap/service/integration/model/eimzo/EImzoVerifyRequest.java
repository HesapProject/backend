package uz.hesap.service.integration.model.eimzo;

// document-service'dan keladigan verify-attached / timestamp so'rovi.
public record EImzoVerifyRequest(String pkcs7, String ipAddress, String host) {}
