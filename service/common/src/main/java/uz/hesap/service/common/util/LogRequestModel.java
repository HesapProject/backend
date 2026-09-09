package uz.hesap.service.common.util;

import java.util.Map;
import java.util.UUID;

public record LogRequestModel(
    UUID id,
    UUID createdBy,
    String createdByName,
    String createdBySurname,
    Map<String, Object> oldModel,
    Map<String, Object> newModel) {}
