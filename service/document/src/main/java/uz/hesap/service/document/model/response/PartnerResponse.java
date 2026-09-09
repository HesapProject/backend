package uz.hesap.service.document.model.response;

import uz.hesap.service.common.util.UserResponse;

// shartnoma hamkori: user ma'lumoti + document soni
public record PartnerResponse(UserResponse user, Long documentCount) {}
