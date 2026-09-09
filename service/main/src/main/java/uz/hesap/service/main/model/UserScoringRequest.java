package uz.hesap.service.main.model;

// Scoring jurnaliga yozish so'rovi (integration -> main /local/user-scoring).
public record UserScoringRequest(
    String scoringId, String scoringType, String requesterIn, String userIn) {}
