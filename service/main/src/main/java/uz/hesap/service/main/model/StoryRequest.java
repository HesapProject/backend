package uz.hesap.service.main.model;

import java.util.List;
import uz.hesap.service.common.util.TextModel;

public record StoryRequest(TextModel title, String avatar, List<StoryItem> items) {}
