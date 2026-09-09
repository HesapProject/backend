package uz.hesap.service.integration.model.eimzo;

// main-service'dan keladigan auth so'rovi (PKCS#7 + client IP).
public record EImzoAuthRequest(String pkcs7, String ipAddress) {}
