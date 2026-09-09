package uz.hesap.service.integration.model.plum;

import java.time.LocalDate;

public record HumoScoringRequestPlum(Long cardId, LocalDate startDate, LocalDate endDate) {}
