package uz.hesap.service.integration.model.plum;

import java.time.LocalDate;

public record CreateScoringCardRequestPlum(
    Long cardId, Long templateId, LocalDate beginDate, LocalDate endDate) {}
