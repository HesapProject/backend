package uz.hesap.service.main.model;

import uz.hesap.service.common.util.TextModel;

public record BannerRequest(
    TextModel title, String image, String link, Integer sortOrder, Boolean isActive) {}
