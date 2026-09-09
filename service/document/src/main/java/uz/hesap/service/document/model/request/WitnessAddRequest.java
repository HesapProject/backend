package uz.hesap.service.document.model.request;

import java.util.UUID;

// Mavjud (imzolanmagan) shartnomaga guvoh qo'shish — contractId + witnessId (user UUID).
public record WitnessAddRequest(UUID contractId, UUID witnessId) {}
