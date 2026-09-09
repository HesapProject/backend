package uz.hesap.service.main.model;

import java.util.UUID;

// Paket foydalanishini qayd etish (s2s, document → main). userId — aktiv user_package'ni
// topish uchun; PINFL (userIn) main'da userId'dan resolish qilinadi.
// userPackageId — foydalanuvchi aniq tanlagan paket (ixtiyoriy; null bo'lsa avto tanlanadi).
public record PackageUsageRequest(
    UUID userId, UUID userPackageId, UUID templateId, UUID contractId) {}
