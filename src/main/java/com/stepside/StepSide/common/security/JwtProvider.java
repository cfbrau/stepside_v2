package com.stepside.StepSide.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class JwtProvider {

    private static final String ROLE_CLAIM = "roles";
    private static final String ROLE_PREFIX = "ROLE_";

    private final SecretKey secretKey;
    private final long expirationTimeMillis;

    public JwtProvider(
            @Value("${stepside.jwt.secret}") String secretString,
            @Value("${stepside.jwt.expiration-hours}") String expirationHoursStr) {

        this.secretKey = Keys.hmacShaKeyFor(secretString.getBytes(StandardCharsets.UTF_8));
        long hours = Long.parseLong(expirationHoursStr.trim());
        this.expirationTimeMillis = hours * 60 * 60 * 1000;
    }

    public String generateToken(String email, String roleName) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTimeMillis);

        return Jwts.builder()
                .subject(email.trim())
                .claims(Map.of(ROLE_CLAIM, roleName.trim().toUpperCase()))
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    public String getEmailFromToken(String token) {
        return extractAllClaims(token).getSubject();
    }

    public List<GrantedAuthority> getAuthoritiesFromToken(String token) {
        Claims claims = extractAllClaims(token);
        String role = claims.get(ROLE_CLAIM, String.class);

        if (role == null || role.isBlank()) {
            return List.of();
        }

        String formattedRole = role.startsWith(ROLE_PREFIX) ? role : ROLE_PREFIX + role;
        return List.of(new SimpleGrantedAuthority(formattedRole));
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SignatureException e) {
            log.error("[AUDITORÍA CRIPTOGRÁFICA] Firma JWT inválida o manipulada fraudulentamente.");
        } catch (MalformedJwtException e) {
            log.error("[AUDITORÍA CRIPTOGRÁFICA] Estructura del Token JWT corrupta o malformada.");
        } catch (ExpiredJwtException e) {
            log.warn("[AUDITORÍA CRIPTOGRÁFICA] Acceso denegado: El Token JWT ha expirado.");
        } catch (UnsupportedJwtException e) {
            log.error("[AUDITORÍA CRIPTOGRÁFICA] Token JWT no soportado por el motor perimetral.");
        } catch (IllegalArgumentException e) {
            log.error("[AUDITORÍA CRIPTOGRÁFICA] El set de Claims del JWT se encuentra vacío.");
        }
        return false;
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
