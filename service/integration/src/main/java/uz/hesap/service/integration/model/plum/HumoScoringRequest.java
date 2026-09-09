package uz.hesap.service.integration.model.plum;

import java.time.LocalDate;
import java.util.UUID;

public record HumoScoringRequest(UUID cardId, LocalDate startDate, LocalDate endDate) {}
