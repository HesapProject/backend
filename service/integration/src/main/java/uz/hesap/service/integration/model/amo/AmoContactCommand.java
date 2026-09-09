package uz.hesap.service.integration.model.amo;

import java.util.UUID;

// amoCRM'ga kontakt yaratish uchun kirish (eski HesapContactModel analogi).
// Consumer buni UserRegisteredEvent'dan quradi; controller /contacts/add ham qabul qiladi.
public record AmoContactCommand(
    String name, String firstName, String lastName, String phone, UUID hesapUserId) {}
