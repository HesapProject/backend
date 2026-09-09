package uz.hesap.service.integration.model.katm;

// POST {base_url}/auth/login — Access/Refresh JWT olish uchun.
public record KatmAuthRequest(String login, String password) {}
