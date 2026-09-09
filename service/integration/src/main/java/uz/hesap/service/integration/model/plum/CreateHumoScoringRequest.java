package uz.hesap.service.integration.model.plum;

public record CreateHumoScoringRequest(
    String phoneNumber, String personCode, String beginDate, String endDate) {}
