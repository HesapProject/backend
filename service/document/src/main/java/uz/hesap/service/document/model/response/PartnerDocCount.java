package uz.hesap.service.document.model.response;

import java.util.UUID;

// ichki DTO: partnerId + doc soni (raw SQL natijasi)
public record PartnerDocCount(UUID partnerId, Long docCount) {}
