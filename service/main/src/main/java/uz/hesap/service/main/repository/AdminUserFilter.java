package uz.hesap.service.main.repository;

import java.time.Instant;
import java.util.UUID;
import uz.hesap.service.common.util.enums.UserType;

// Admin foydalanuvchilar ro'yxati filtri — barcha optional shartlar bir joyda.
public record AdminUserFilter(
    Boolean deleted,
    String search,
    UUID companyId,
    UserType type,
    Boolean isVerified,
    Integer contractCountFrom,
    Integer contractCountTo,
    Double balanceFrom,
    Double balanceTo,
    Instant lastVisitFrom,
    Instant lastVisitTo,
    Instant createdFrom,
    Instant createdTo,
    // true bo'lsa — faqat haqiqiy PINFL (JShShIR)ga ega mijozlar. Statistika uchun:
    // PINFL'siz (telefon bilan ro'yxatdan o'tgan, tasdiqlanmagan) userlar sanalmaydi.
    Boolean hasPinfl) {}
