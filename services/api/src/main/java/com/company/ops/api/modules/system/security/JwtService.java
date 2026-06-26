package com.company.ops.api.modules.system.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  private final SecretKey secretKey;
  private final long expireMinutes;

  public JwtService(
      @Value("${ops.security.jwt-secret}") String secret,
      @Value("${ops.security.jwt-expire-minutes}") long expireMinutes
  ) {
    this.secretKey = buildKey(secret);
    this.expireMinutes = expireMinutes;
  }

  public String createToken(UserPrincipal principal) {
    Instant now = Instant.now();
    Instant expiresAt = now.plusSeconds(expireMinutes * 60);
    return Jwts.builder()
        .subject(principal.getUsername())
        .claim("uid", principal.id().toString())
        .claim("name", principal.displayName())
        .claim("roles", principal.roleCodes())
        .claim("permissions", principal.permissions())
        .issuedAt(Date.from(now))
        .expiration(Date.from(expiresAt))
        .signWith(secretKey)
        .compact();
  }

  public String extractUsername(String token) {
    return parseClaims(token).getSubject();
  }

  public boolean isValid(String token, UserPrincipal principal) {
    Claims claims = parseClaims(token);
    return principal.getUsername().equals(claims.getSubject())
        && claims.getExpiration().after(new Date());
  }

  private Claims parseClaims(String token) {
    return Jwts.parser()
        .verifyWith(secretKey)
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

  private SecretKey buildKey(String secret) {
    byte[] bytes;
    try {
      bytes = Decoders.BASE64.decode(secret);
    } catch (DecodingException | IllegalArgumentException ignored) {
      bytes = secret.getBytes(StandardCharsets.UTF_8);
    }
    return Keys.hmacShaKeyFor(bytes);
  }
}

