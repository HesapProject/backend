package uz.hesap.service.common.util;

import java.util.UUID;
import uz.hesap.service.common.util.enums.UserType;

// PDF/Jasper template'lar uchun "party" (tomon) ma'lumotlari — OneID + user'dan yig'iladi.
// DocumentGenerator har "party"'ni Map sifatida yetkazadi: $P{seller}.get("fullName"),
// $P{buyer}.get("legalName"), va h.k. Shu sabab field'lar ko'p — template muallifi
// xohlaganini tanlasin.
public record UserPassportBasicResponse(
    UUID userId,
    String fullName,
    String firstName,
    String lastName,
    String midName,
    String document,
    String in,
    String phone,
    String address,
    // COMPANY (yuridik shaxs) uchun:
    String legalName,
    String tin,
    // COMPANY tomoni bo'lsa — direktor (OWNER staff) F.I.SH'i. Aks holda bo'sh.
    String ownerFullName,
    String ownerFirstName,
    String ownerLastName,
    String ownerMidName,
    // Universal markerlar:
    UserType type,
    Boolean isVerified) {}
