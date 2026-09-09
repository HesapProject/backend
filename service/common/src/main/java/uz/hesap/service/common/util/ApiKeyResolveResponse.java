package uz.hesap.service.common.util;

// `POST /main/v1/local/api-keys/resolve` javobi — X-API-Key headerini kalit egasi
// (owner) va uning huquqlariga (apiKey) aylantiradi. Document servisning
// ApiKeyAuthConverter'i shu javobdan UserPrincipal quradi.
public record ApiKeyResolveResponse(UserResponse owner, ApiKeyContext apiKey) {}
