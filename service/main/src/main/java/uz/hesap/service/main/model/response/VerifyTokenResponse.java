package uz.hesap.service.main.model.response;

/**
 * Response for verify code endpoint. Contains a temporary token that can only be used to create a
 * user.
 */
public record VerifyTokenResponse(String verifyToken) {}
