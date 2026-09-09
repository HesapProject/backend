package uz.hesap.service.document.model.request;

import uz.hesap.service.document.model.enums.ActionType;

public record VerificationSmsRequest(ActionType action) {}
