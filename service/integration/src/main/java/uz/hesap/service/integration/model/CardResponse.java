package uz.hesap.service.integration.model;

import java.util.UUID;
import uz.hesap.service.integration.domain.enums.CardType;

public record CardResponse(
    UUID id, String userIn, String cardNumber, String expireDate, CardType type) {}
