package uz.hesap.service.integration.model.plum;

public record CreateScoringRequest(
    String personCode, Integer templateId, String beginDate, String endDate, String phoneNumber) {}
