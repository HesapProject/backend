package uz.hesap.service.integration.model;

// Fuqaroni SMS orqali taklif qilish so'rovi: telefon + til (uz/ru/eng).
public record SendInviteRequest(String phone, String language) {}
