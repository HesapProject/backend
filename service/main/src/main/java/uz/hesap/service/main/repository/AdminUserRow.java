package uz.hesap.service.main.repository;

import java.time.Instant;
import uz.hesap.service.main.domain.UserEntity;

// Ro'yxat query natijasi — user + cross-schema agregatlar (shartnoma soni,
// SUMMA balans, oxirgi tashrif).
public record AdminUserRow(
    UserEntity user, Integer contractsCount, Double balance, Instant lastVisitDate) {}
