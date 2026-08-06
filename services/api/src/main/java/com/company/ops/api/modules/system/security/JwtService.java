package com.company.ops.api.modules.system.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import com.company.ops.api.modules.procurement.domain.SupplierPortalAccount;
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
        .claim("tenant", principal.tenantId())
        .claim("name", principal.displayName())
        .claim("roles", principal.roleCodes())
        .claim("permissions", principal.permissions())
        .claim("ver", principal.authVersion())
        .issuedAt(Date.from(now))
        .expiration(Date.from(expiresAt))
        .signWith(secretKey)
        .compact();
  }

  public String createSupplierPortalToken(SupplierPortalAccount account) {
    Instant now = Instant.now();
    Instant expiresAt = now.plusSeconds(expireMinutes * 60);
    return Jwts.builder()
        .subject(account.getEmail())
        .claim("type", "SUPPLIER_PORTAL")
        .claim("aid", account.getId().toString())
        .claim("sid", account.getSupplierId().toString())
        .claim("tenant", account.getTenantId())
        .claim("name", account.getContactName())
        .claim("ver", account.getAuthVersion())
        .issuedAt(Date.from(now))
        .expiration(Date.from(expiresAt))
        .signWith(secretKey)
        .compact();
  }

  public String extractUsername(String token) {
    return parseClaims(token).getSubject();
  }

  public String extractTenant(String token) {
    return parseClaims(token).get("tenant", String.class);
  }

  public boolean isSupplierPortalToken(String token) {
    return "SUPPLIER_PORTAL".equals(parseClaims(token).get("type", String.class));
  }

  public UUID extractPortalAccountId(String token) {
    return UUID.fromString(parseClaims(token).get("aid", String.class));
  }

  public boolean isValidSupplierPortalToken(String token, SupplierPortalAccount account) {
    Claims claims = parseClaims(token);
    Number tokenVersion = claims.get("ver", Number.class);
    return !"REJECTED".equals(account.getStatus())
        && !"SUSPENDED".equals(account.getStatus())
        && account.getEmail().equalsIgnoreCase(claims.getSubject())
        && account.getId().toString().equals(claims.get("aid", String.class))
        && account.getSupplierId().toString().equals(claims.get("sid", String.class))
        && tokenVersion != null
        && tokenVersion.longValue() == account.getAuthVersion()
        && claims.getExpiration().after(new Date());
  }

  public boolean isValid(String token, UserPrincipal principal) {
    Claims claims = parseClaims(token);
    Number tokenVersion = claims.get("ver", Number.class);
    return principal.isEnabled()
        && principal.getUsername().equals(claims.getSubject())
        && tokenVersion != null
        && tokenVersion.longValue() == principal.authVersion()
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
    byte[] bytes = secret.startsWith("base64:")
        ? Decoders.BASE64.decode(secret.substring("base64:".length()))
        : secret.getBytes(StandardCharsets.UTF_8);
    return Keys.hmacShaKeyFor(bytes);
  }
}
