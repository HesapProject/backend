package uz.hesap.service.integration.model.oneid;

// sso.egov.uz qaytaradigan OAuth token javobi.
public record TokenResponse(
    String scope, long expires_in, String token_type, String refresh_token, String access_token) {}
