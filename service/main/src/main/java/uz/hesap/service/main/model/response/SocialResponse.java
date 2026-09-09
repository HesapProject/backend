package uz.hesap.service.main.model.response;

import java.util.UUID;

public record SocialResponse(
    UUID id, String userIn, String phone2, String telegram, String instagram, String facebook) {}
