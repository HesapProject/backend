package uz.hesap.service.main.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
  @Value("${application.token.signing.key}")
  private String jwtSigningKey;

  public String extractSubject(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  public Claims getAllClaims(String token) {
    return extractAllClaims(token);
  }

  public String generateTokenTemporary(UUID userId, UUID sessionId, String iss) {
    return generateTemporaryToken(
        Map.of("userId", userId, "sessionId", sessionId), userId.toString(), iss);
  }

  public String generateTokenTemporary(
      UUID userId, UUID sessionId, UUID companyId, String iss) {
    return generateTemporaryToken(
        Map.of("userId", userId, "sessionId", sessionId, "companyId", companyId),
        userId.toString(),
        iss);
  }

  private String generateTemporaryToken(Map<String, Object> extraClaims, String sub, String iss) {
    return Jwts.builder()
        .claims(extraClaims)
        .subject(sub)
        .issuer(iss)
        .issuedAt(new Date(System.currentTimeMillis()))
        .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 5))
        .signWith(getSigningKey())
        .compact();
  }

  public String generateToken(UUID userId, UUID sessionId, String iss) {
    return generateToken(
        Map.of("userId", userId, "sessionId", sessionId), userId.toString(), iss);
  }

  public boolean isTokenValid(String token, String sub) {
    final String subject = extractSubject(token);
    return (sub.equals(subject)) && !isTokenExpired(token);
  }

  private <T> T extractClaim(String token, Function<Claims, T> claimsResolvers) {
    final Claims claims = extractAllClaims(token);
    return claimsResolvers.apply(claims);
  }

  public String generateToken(Map<String, Object> extraClaims, String sub, String iss) {
    return Jwts.builder()
        .claims(extraClaims)
        .subject(sub)
        .issuer(iss)
        .issuedAt(new Date(System.currentTimeMillis()))
        .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24))
        .signWith(getSigningKey())
        .compact();
  }

  private boolean isTokenExpired(String token) {
    return extractExpiration(token).before(new Date());
  }

  private Date extractExpiration(String token) {
    return extractClaim(token, Claims::getExpiration);
  }

  private Claims extractAllClaims(String token) {
    return Jwts.parser()
        .setSigningKey(getSigningKey())
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

  private SecretKey getSigningKey() {
    byte[] keyBytes = Decoders.BASE64.decode(jwtSigningKey);
    return Keys.hmacShaKeyFor(keyBytes);
  }

  /**
   * Generate a temporary verify token for user creation flow. This token is valid for 10 minutes
   * and can only be used for sign-up.
   */
  public String generateVerifyToken(UUID sessionId, String phone) {
    return Jwts.builder()
        .claims(Map.of("sessionId", sessionId, "phone", phone, "purpose", "verify"))
        .subject(sessionId.toString())
        .issuer("verify-flow")
        .issuedAt(new Date(System.currentTimeMillis()))
        .expiration(new Date(System.currentTimeMillis() + 1000L * 60 * 100000)) // 10 minutes
        .signWith(getSigningKey())
        .compact();
  }

  /** Validate if the token is a verify token */
  public boolean isVerifyToken(Claims claims) {
    String purpose = claims.get("purpose", String.class);
    return "verify".equals(purpose);
  }

  /** Extract session ID from verify token */
  public UUID extractSessionIdFromVerifyToken(Claims claims) {
    return UUID.fromString(claims.get("sessionId", String.class));
  }

  // 30 kunlik token (OneID verify uchun)
  public String generateLongLivedToken(UUID userId, UUID sessionId, String iss) {
    return Jwts.builder()
        .claims(Map.of("userId", userId, "sessionId", sessionId))
        .subject(userId.toString())
        .issuer(iss)
        .issuedAt(new Date(System.currentTimeMillis()))
        .expiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 30)) // 30 kun
        .signWith(getSigningKey())
        .compact();
  }
}
