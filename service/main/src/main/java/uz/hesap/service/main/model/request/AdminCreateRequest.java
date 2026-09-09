package uz.hesap.service.main.model.request;

// Control admin akkaunt yaratish — ism/familiya + login/parol. type=ADMIN.
public record AdminCreateRequest(
    String firstName, String lastName, String login, String password) {}
