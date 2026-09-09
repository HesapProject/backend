package uz.hesap.service.main.model.request;

// E-IMZO orqali yuridik shaxsni tasdiqlash: frontend E-IMZO'dan olingan PKCS#7.
public record EImzoVerifyRequest(String pkcs7) {}
