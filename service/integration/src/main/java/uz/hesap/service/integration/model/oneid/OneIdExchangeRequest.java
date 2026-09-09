package uz.hesap.service.integration.model.oneid;

// main-service code → user data ayirboshlash uchun yuboradi.
public record OneIdExchangeRequest(String code, String redirectUri) {}
